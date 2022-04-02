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
package com.lifeofcoder.autolimiter.cluster.server.handler;

import com.alibaba.csp.sentinel.util.StringUtil;
import com.lifeofcoder.autolimiter.cluster.common.ClusterConstants;
import com.lifeofcoder.autolimiter.cluster.common.request.ClusterRequest;
import com.lifeofcoder.autolimiter.cluster.common.response.ClusterResponse;
import com.lifeofcoder.autolimiter.cluster.server.connection.ConnectionManager;
import com.lifeofcoder.autolimiter.cluster.server.connection.ConnectionPool;
import com.lifeofcoder.autolimiter.cluster.server.processor.RequestProcessor;
import com.lifeofcoder.autolimiter.cluster.server.processor.RequestProcessorProvider;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * Netty server handler for Sentinel token server.
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
public class TokenServerHandler extends ChannelInboundHandlerAdapter {
    private static Logger LOGGER = LoggerFactory.getLogger(TokenServerHandler.class);

    private final ConnectionPool globalConnectionPool;

    public TokenServerHandler(ConnectionPool globalConnectionPool) {
        this.globalConnectionPool = globalConnectionPool;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        globalConnectionPool.createConnection(ctx.channel());
        String remoteAddress = getRemoteAddress(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String remoteAddress = getRemoteAddress(ctx);
        globalConnectionPool.remove(ctx.channel());
        ConnectionManager.removeConnection(remoteAddress);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        globalConnectionPool.refreshLastReadTime(ctx.channel());
        if (msg instanceof ClusterRequest) {
            ClusterRequest request = (ClusterRequest) msg;

            // Client ping with its namespace, add to connection manager.
            if (request.getType() == ClusterConstants.MSG_TYPE_PING) {
                handlePingRequest(ctx, request);
                return;
            }

            // Pick request processor for request type.
            RequestProcessor<?, ?> processor = RequestProcessorProvider.getProcessor(request.getType());
            if (processor == null) {
                LOGGER.warn("[TokenServerHandler] No processor for request type: " + request.getType());
                writeBadResponse(ctx, request);
            }
            else {
                try {
                    ClusterResponse<?> response = processor.processRequest(request);
                    writeResponse(ctx, response);
                }
                catch (Exception e) {
                    LOGGER.error("Failed to process request with processor[" + processor.getClass().getName() + "].", e);
                    writeBadResponse(ctx, request);
                }
            }
        }
    }

    private void writeBadResponse(ChannelHandlerContext ctx, ClusterRequest request) {
        ClusterResponse<?> response = new ClusterResponse<>(request.getId(), request.getType(), ClusterConstants.RESPONSE_STATUS_BAD, null);
        writeResponse(ctx, response);
    }

    private void writeResponse(ChannelHandlerContext ctx, ClusterResponse response) {
        ctx.writeAndFlush(response);
    }

    private void handlePingRequest(ChannelHandlerContext ctx, ClusterRequest request) {
        if (request.getData() == null || StringUtil.isBlank((String) request.getData())) {
            writeBadResponse(ctx, request);
            return;
        }
        String namespace = (String) request.getData();
        String clientAddress = getRemoteAddress(ctx);
        // Add the remote namespace to connection manager.
        int curCount = ConnectionManager.addConnection(namespace, clientAddress).getConnectedCount();
        int status = ClusterConstants.RESPONSE_STATUS_OK;
        ClusterResponse<Integer> response = new ClusterResponse<>(request.getId(), request.getType(), status, curCount);
        writeResponse(ctx, response);
    }

    private String getRemoteAddress(ChannelHandlerContext ctx) {
        if (ctx.channel().remoteAddress() == null) {
            return null;
        }
        InetSocketAddress inetAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        return inetAddress.getAddress().getHostAddress() + ":" + inetAddress.getPort();
    }
}
