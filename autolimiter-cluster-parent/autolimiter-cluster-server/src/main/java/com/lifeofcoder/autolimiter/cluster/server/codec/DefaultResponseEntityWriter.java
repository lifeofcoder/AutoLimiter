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
package com.lifeofcoder.autolimiter.cluster.server.codec;

import com.lifeofcoder.autolimiter.cluster.common.ClusterConstants;
import com.lifeofcoder.autolimiter.cluster.common.codec.EntityWriter;
import com.lifeofcoder.autolimiter.cluster.common.codec.response.ResponseEntityWriter;
import com.lifeofcoder.autolimiter.cluster.common.response.ClusterResponse;
import com.lifeofcoder.autolimiter.cluster.common.response.Response;
import com.lifeofcoder.autolimiter.cluster.server.codec.registry.ResponseDataWriterRegistry;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class DefaultResponseEntityWriter implements ResponseEntityWriter<ClusterResponse, ByteBuf> {
    private static Logger LOGGER = LoggerFactory.getLogger(DefaultResponseEntityWriter.class);

    @Override
    public void writeTo(ClusterResponse response, ByteBuf out) {
        int type = response.getType();
        EntityWriter<Object, ByteBuf> responseDataWriter = ResponseDataWriterRegistry.getWriter(type);

        if (responseDataWriter == null) {
            writeHead(response.setStatus(ClusterConstants.RESPONSE_STATUS_BAD), out);
            LOGGER.warn("[NettyResponseEncoder] Cannot find matching writer for type <{}>", response.getType());
            return;
        }
        writeHead(response, out);
        responseDataWriter.writeTo(response.getData(), out);
    }

    private void writeHead(Response response, ByteBuf out) {
        out.writeInt(response.getId());
        out.writeByte(response.getType());
        out.writeByte(response.getStatus());
    }
}
