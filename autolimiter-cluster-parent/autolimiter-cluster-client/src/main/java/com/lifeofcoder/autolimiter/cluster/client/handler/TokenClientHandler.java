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
package com.lifeofcoder.autolimiter.cluster.client.handler;

import com.lifeofcoder.autolimiter.cluster.client.ClientConstants;
import com.lifeofcoder.autolimiter.cluster.common.ClusterConstants;
import com.lifeofcoder.autolimiter.cluster.common.request.ClusterRequest;
import com.lifeofcoder.autolimiter.cluster.common.response.ClusterResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Netty client handler for Sentinel token client.
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
public class TokenClientHandler extends ChannelInboundHandlerAdapter {
    private static Logger LOGGER = LoggerFactory.getLogger(TokenClientHandler.class);

    private final AtomicInteger currentState;
    private final Runnable disconnectCallback;

    public TokenClientHandler(AtomicInteger currentState, Runnable disconnectCallback) {
        this.currentState = currentState;
        this.disconnectCallback = disconnectCallback;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        currentState.set(ClientConstants.CLIENT_STATUS_STARTED);
        fireClientPing(ctx);
        LOGGER.info("[TokenClientHandler] Client handler active, remote address: " + getRemoteAddress(ctx));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ClusterResponse) {
            ClusterResponse<?> response = (ClusterResponse) msg;

            if (response.getType() == ClusterConstants.MSG_TYPE_PING) {
                handlePingResponse(ctx, response);
                return;
            }

            TokenClientPromiseHolder.completePromise(response.getId(), response);
        }
    }

    private void fireClientPing(ChannelHandlerContext ctx) {
        // Data body: namespace of the client.
        ClusterRequest<String> ping = new ClusterRequest<String>().setId(0).setType(ClusterConstants.MSG_TYPE_PING).setData("default"); //namespace写死
        //.setData(ConfigSupplierRegistry.getNamespaceSupplier().get());
        ctx.writeAndFlush(ping);
    }

    private void handlePingResponse(ChannelHandlerContext ctx, ClusterResponse response) {
        if (response.getStatus() == ClusterConstants.RESPONSE_STATUS_OK) {
            int count = (int) response.getData();
            LOGGER.info("[TokenClientHandler] Client ping OK (target server: {}, connected count: {})", getRemoteAddress(ctx), count);
        }
        else {
            LOGGER.warn("[TokenClientHandler] Client ping failed (target server: {})", getRemoteAddress(ctx));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.warn("[TokenClientHandler] Client exception caught", cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("[TokenClientHandler] Client handler inactive, remote address: " + getRemoteAddress(ctx));
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("[TokenClientHandler] Client channel unregistered, remote address: " + getRemoteAddress(ctx));
        currentState.set(ClientConstants.CLIENT_STATUS_OFF);

        disconnectCallback.run();
    }

    private String getRemoteAddress(ChannelHandlerContext ctx) {
        if (ctx.channel().remoteAddress() == null) {
            return null;
        }
        InetSocketAddress inetAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        return inetAddress.getAddress().getHostAddress() + ":" + inetAddress.getPort();
    }

    public int getCurrentState() {
        return currentState.get();
    }

    public boolean hasStarted() {
        return getCurrentState() == ClientConstants.CLIENT_STATUS_STARTED;
    }
}
