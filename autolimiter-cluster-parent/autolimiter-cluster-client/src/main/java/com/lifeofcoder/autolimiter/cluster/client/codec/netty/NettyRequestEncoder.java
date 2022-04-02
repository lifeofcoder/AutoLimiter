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
package com.lifeofcoder.autolimiter.cluster.client.codec.netty;

import com.lifeofcoder.autolimiter.cluster.client.codec.ClientEntityCodecProvider;
import com.lifeofcoder.autolimiter.cluster.common.codec.request.RequestEntityWriter;
import com.lifeofcoder.autolimiter.cluster.common.request.ClusterRequest;
import com.lifeofcoder.autolimiter.cluster.common.request.Request;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class NettyRequestEncoder extends MessageToByteEncoder<ClusterRequest> {
    private static Logger LOGGER = LoggerFactory.getLogger(NettyRequestEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ClusterRequest request, ByteBuf out) throws Exception {
        RequestEntityWriter<Request, ByteBuf> requestEntityWriter = ClientEntityCodecProvider.getRequestEntityWriter();
        if (requestEntityWriter == null) {
            LOGGER.warn("[NettyRequestEncoder] Cannot resolve the global request entity writer, dropping the request");
            return;
        }

        requestEntityWriter.writeTo(request, out);
    }
}
