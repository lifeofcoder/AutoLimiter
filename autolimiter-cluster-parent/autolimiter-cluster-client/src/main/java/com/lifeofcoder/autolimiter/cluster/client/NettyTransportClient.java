/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lifeofcoder.autolimiter.cluster.client;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.lifeofcoder.autolimiter.cluster.client.codec.netty.NettyRequestEncoder;
import com.lifeofcoder.autolimiter.cluster.client.codec.netty.NettyResponseDecoder;
import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.cluster.client.handler.TokenClientHandler;
import com.lifeofcoder.autolimiter.cluster.client.handler.TokenClientPromiseHolder;
import com.lifeofcoder.autolimiter.cluster.common.ClusterErrorMessages;
import com.lifeofcoder.autolimiter.cluster.common.ClusterTransportClient;
import com.lifeofcoder.autolimiter.cluster.common.exception.SentinelClusterException;
import com.lifeofcoder.autolimiter.cluster.common.request.ClusterRequest;
import com.lifeofcoder.autolimiter.cluster.common.request.Request;
import com.lifeofcoder.autolimiter.cluster.common.response.ClusterResponse;
import com.lifeofcoder.autolimiter.common.utils.OsHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Netty transport client implementation for Sentinel cluster transport.
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
public class NettyTransportClient implements ClusterTransportClient {
    private static Logger LOGGER = LoggerFactory.getLogger(NettyTransportClient.class);

    @SuppressWarnings("PMD.ThreadPoolCreationRule")
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1, new NamedThreadFactory("sentinel-cluster-transport-client-scheduler"));

    private static final int DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() + 1));

    public static final int RECONNECT_DELAY_MS = 2000;

    private final String host;
    private final int port;

    private Channel channel;
    private EventLoopGroup eventLoopGroup;
    private TokenClientHandler clientHandler;

    private final AtomicInteger idGenerator = new AtomicInteger(0);
    private final AtomicInteger currentState = new AtomicInteger(ClientConstants.CLIENT_STATUS_OFF);
    private final AtomicInteger failConnectedTime = new AtomicInteger(0);

    private final AtomicBoolean shouldRetry = new AtomicBoolean(true);

    public NettyTransportClient(String host, int port) {
        AssertUtil.assertNotBlank(host, "remote host cannot be blank");
        AssertUtil.isTrue(port > 0, "port should be positive");
        this.host = host;
        this.port = port;
    }

    private Bootstrap initClientBootstrap() {
        int workThreads = DEFAULT_EVENT_LOOP_THREADS;
        Bootstrap b = new Bootstrap();
        if (OsHelper.getOsType() == OsHelper.OsType.LINUX) {
            eventLoopGroup = new EpollEventLoopGroup(workThreads);
            b.channel(EpollSocketChannel.class); //NioSocketChannel
        }
        else {
            eventLoopGroup = new NioEventLoopGroup(workThreads);
            b.channel(NioSocketChannel.class); //NioSocketChannel
        }
        b.group(eventLoopGroup).option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, ClusterClientConfigManager.getConnectTimeout())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        clientHandler = new TokenClientHandler(currentState, disconnectCallback);

                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 2, 0, 2));
                        pipeline.addLast(new NettyResponseDecoder());
                        pipeline.addLast(new LengthFieldPrepender(2));
                        pipeline.addLast(new NettyRequestEncoder());
                        pipeline.addLast(clientHandler);
                    }
                });

        return b;
    }

    private void connect(Bootstrap b) {
        if (currentState.compareAndSet(ClientConstants.CLIENT_STATUS_OFF, ClientConstants.CLIENT_STATUS_PENDING)) {
            b.connect(host, port).addListener(new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.cause() != null) {
                        LOGGER.warn(String.format("[NettyTransportClient] Could not connect to <%s:%d> after %d times", host, port, failConnectedTime.get()), future.cause());
                        failConnectedTime.incrementAndGet();
                        channel = null;
                    }
                    else {
                        failConnectedTime.set(0);
                        channel = future.channel();
                        LOGGER.info("[NettyTransportClient] Successfully connect to server <" + host + ":" + port + ">");
                    }
                }
            });
        }
    }

    private Runnable disconnectCallback = new Runnable() {
        @Override
        public void run() {
            if (!shouldRetry.get()) {
                return;
            }
            SCHEDULER.schedule(new Runnable() {
                @Override
                public void run() {
                    if (shouldRetry.get()) {
                        LOGGER.info("[NettyTransportClient] Reconnecting to server <" + host + ":" + port + ">");
                        try {
                            startInternal();
                        }
                        catch (Exception e) {
                            LOGGER.warn("[NettyTransportClient] Failed to reconnect to server", e);
                        }
                    }
                }
            }, RECONNECT_DELAY_MS * (failConnectedTime.get() + 1), TimeUnit.MILLISECONDS);
            cleanUp();
        }
    };

    @Override
    public void start() throws Exception {
        shouldRetry.set(true);
        startInternal();
    }

    private void startInternal() {
        connect(initClientBootstrap());
    }

    private void cleanUp() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
    }

    @Override
    public void stop() throws Exception {
        // Stop retrying for connection.
        shouldRetry.set(false);

        while (currentState.get() == ClientConstants.CLIENT_STATUS_PENDING) {
            try {
                Thread.sleep(200);
            }
            catch (Exception ex) {
                // Ignore.
            }
        }

        cleanUp();
        failConnectedTime.set(0);

        LOGGER.info("[NettyTransportClient] Cluster transport client stopped");
    }

    private boolean validRequest(Request request) {
        return request != null && request.getType() >= 0;
    }

    @Override
    public boolean isReady() {
        return channel != null && clientHandler != null && clientHandler.hasStarted();
    }

    @Override
    public ClusterResponse sendRequest(ClusterRequest request) throws Exception {
        if (!isReady()) {
            throw new SentinelClusterException(ClusterErrorMessages.CLIENT_NOT_READY);
        }
        if (!validRequest(request)) {
            throw new SentinelClusterException(ClusterErrorMessages.BAD_REQUEST);
        }
        int xid = getCurrentId();
        try {
            request.setId(xid);

            channel.writeAndFlush(request);

            ChannelPromise promise = channel.newPromise();
            TokenClientPromiseHolder.putPromise(xid, promise);

            if (!promise.await(ClusterClientConfigManager.getRequestTimeout())) {
                throw new SentinelClusterException(ClusterErrorMessages.REQUEST_TIME_OUT);
            }

            SimpleEntry<ChannelPromise, ClusterResponse> entry = TokenClientPromiseHolder.getEntry(xid);
            if (entry == null || entry.getValue() == null) {
                // Should not go through here.
                throw new SentinelClusterException(ClusterErrorMessages.UNEXPECTED_STATUS);
            }
            return entry.getValue();
        }
        finally {
            TokenClientPromiseHolder.remove(xid);
        }
    }

    private int getCurrentId() {
        if (idGenerator.get() > MAX_ID) {
            idGenerator.set(0);
        }
        return idGenerator.incrementAndGet();
    }

    /*public CompletableFuture<ClusterResponse> sendRequestAsync(ClusterRequest request) {
        // Uncomment this when min target JDK is 1.8.
        if (!validRequest(request)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Bad request"));
        }
        int xid = getCurrentId();
        request.setId(xid);

        CompletableFuture<ClusterResponse> future = new CompletableFuture<>();
        channel.writeAndFlush(request)
            .addListener(f -> {
                if (f.isSuccess()) {
                    future.complete(someResult);
                } else if (f.cause() != null) {
                    future.completeExceptionally(f.cause());
                } else {
                    future.cancel(false);
                }
            });
        return future;
    }*/

    private static final int MAX_ID = 999_999_999;
}
