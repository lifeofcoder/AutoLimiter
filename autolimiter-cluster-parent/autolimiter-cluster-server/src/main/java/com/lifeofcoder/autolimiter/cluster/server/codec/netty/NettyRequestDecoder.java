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
package com.lifeofcoder.autolimiter.cluster.server.codec.netty;

import com.lifeofcoder.autolimiter.cluster.common.codec.request.RequestEntityDecoder;
import com.lifeofcoder.autolimiter.cluster.common.request.Request;
import com.lifeofcoder.autolimiter.cluster.server.codec.ServerEntityCodecProvider;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class NettyRequestDecoder extends ByteToMessageDecoder {
    private static Logger LOGGER = LoggerFactory.getLogger(NettyRequestDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        RequestEntityDecoder<ByteBuf, Request> requestDecoder = ServerEntityCodecProvider.getRequestEntityDecoder();
        if (requestDecoder == null) {
            LOGGER.warn("[NettyRequestDecoder] Cannot resolve the global request entity decoder, " + "dropping the request");
            return;
        }

        // TODO: handle decode error here.
        Request request = requestDecoder.decode(in);
        if (request != null) {
            out.add(request);
        }
    }
}
