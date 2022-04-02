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
package com.lifeofcoder.autolimiter.cluster.server;

import com.alibaba.csp.sentinel.cluster.server.ClusterTokenServer;
import com.lifeofcoder.autolimiter.cluster.server.codec.netty.NettyRequestDecoder;
import com.lifeofcoder.autolimiter.cluster.server.codec.netty.NettyResponseEncoder;
import com.lifeofcoder.autolimiter.cluster.server.config.ServerTransportConfig;
import com.lifeofcoder.autolimiter.cluster.server.connection.Connection;
import com.lifeofcoder.autolimiter.cluster.server.connection.ConnectionPool;
import com.lifeofcoder.autolimiter.cluster.server.handler.TokenServerHandler;
import com.lifeofcoder.autolimiter.common.utils.OsHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.lifeofcoder.autolimiter.cluster.server.ServerConstants.*;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class NettyTransportServer implements ClusterTokenServer {
    private static Logger LOGGER = LoggerFactory.getLogger(NettyTransportServer.class);

    private static final int MAX_RETRY_TIMES = 3;
    private static final int RETRY_SLEEP_MS = 2000;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private final ConnectionPool connectionPool = new ConnectionPool();

    private final AtomicInteger currentState = new AtomicInteger(SERVER_STATUS_OFF);
    private final AtomicInteger failedTimes = new AtomicInteger(0);

    private ServerTransportConfig serverTransportConfig;

    public NettyTransportServer(int port) {
        serverTransportConfig = new ServerTransportConfig();
        serverTransportConfig.setPort(port);
    }

    public NettyTransportServer(ServerTransportConfig serverTransportConfig) {
        this.serverTransportConfig = serverTransportConfig;
    }

    @Override
    public void start() {
        if (!currentState.compareAndSet(SERVER_STATUS_OFF, SERVER_STATUS_STARTING)) {
            return;
        }

        LOGGER.info("The server transport config of cluster is " + serverTransportConfig);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        //NioEventLoopGroup
        if (useEpoll()) {
            LOGGER.info("Use epoll to start netty transport server.");
            this.bossGroup = new EpollEventLoopGroup(serverTransportConfig.getBossThreads());
            this.workerGroup = new EpollEventLoopGroup(serverTransportConfig.getWorkThreads());
            serverBootstrap.channel(EpollServerSocketChannel.class); //
        }
        else {
            LOGGER.info("Use nio selector to start netty transport server.");
            this.bossGroup = new NioEventLoopGroup(serverTransportConfig.getBossThreads());
            this.workerGroup = new NioEventLoopGroup(serverTransportConfig.getWorkThreads());
            serverBootstrap.channel(NioServerSocketChannel.class); //NioServerSocketChannel
        }
        serverBootstrap.group(bossGroup, workerGroup).option(ChannelOption.SO_BACKLOG, serverTransportConfig.getSoBacklog()).handler(new LoggingHandler(LogLevel.INFO)).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 2, 0, 2));
                p.addLast(new NettyRequestDecoder());
                p.addLast(new LengthFieldPrepender(2));
                p.addLast(new NettyResponseEncoder());
                p.addLast(new TokenServerHandler(connectionPool));
            }
        }).childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).childOption(ChannelOption.SO_SNDBUF, serverTransportConfig.getSoSndbuf())
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, serverTransportConfig.getConnectTimeoutMillis())
                //            .childOption(ChannelOption.SO_TIMEOUT, 10) //: Unknown channel option
                .childOption(ChannelOption.TCP_NODELAY, serverTransportConfig.isTcpNodelay()).childOption(ChannelOption.SO_RCVBUF, serverTransportConfig.getSoRcvbuf());

        serverBootstrap.bind(serverTransportConfig.getPort()).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.cause() != null) {
                    LOGGER.info("[NettyTransportServer] Token server start failed (port=" + serverTransportConfig.getPort() + "), failedTimes: " + failedTimes.get(), future.cause());
                    currentState.compareAndSet(SERVER_STATUS_STARTING, SERVER_STATUS_OFF);
                    int failCount = failedTimes.incrementAndGet();
                    if (failCount > MAX_RETRY_TIMES) {
                        return;
                    }

                    try {
                        Thread.sleep(failCount * RETRY_SLEEP_MS);
                        start();
                    }
                    catch (Throwable e) {
                        LOGGER.info("[NettyTransportServer] Failed to start token server when retrying", e);
                    }
                }
                else {
                    LOGGER.info("[NettyTransportServer] Token server started success at port " + serverTransportConfig.getPort());
                    currentState.compareAndSet(SERVER_STATUS_STARTING, SERVER_STATUS_STARTED);
                }
            }
        });
    }

    private boolean useEpoll() {
        return OsHelper.getOsType() == OsHelper.OsType.LINUX && serverTransportConfig.isEpoll();
    }

    @Override
    public void stop() {
        // If still initializing, wait for ready.
        while (currentState.get() == SERVER_STATUS_STARTING) {
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                // Ignore.
            }
        }

        if (currentState.compareAndSet(SERVER_STATUS_STARTED, SERVER_STATUS_OFF)) {
            try {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                connectionPool.shutdownAll();

                failedTimes.set(0);

                LOGGER.info("[NettyTransportServer] Sentinel token server stopped");
            }
            catch (Exception ex) {
                LOGGER.warn("[NettyTransportServer] Failed to stop token server (port=" + serverTransportConfig.getPort() + ")", ex);
            }
        }
    }

    public void refreshRunningServer() {
        connectionPool.refreshIdleTask();
    }

    public void closeConnection(String clientIp, int clientPort) throws Exception {
        Connection connection = connectionPool.getConnection(clientIp, clientPort);
        connection.close();
    }

    public void closeAll() throws Exception {
        List<Connection> connections = connectionPool.listAllConnection();
        for (Connection connection : connections) {
            connection.close();
        }
    }

    public List<String> listAllClient() {
        List<String> clients = new ArrayList<String>();
        List<Connection> connections = connectionPool.listAllConnection();
        for (Connection conn : connections) {
            clients.add(conn.getConnectionKey());
        }
        return clients;
    }

    public int getCurrentState() {
        return currentState.get();
    }

    public int clientCount() {
        return connectionPool.count();
    }
}
