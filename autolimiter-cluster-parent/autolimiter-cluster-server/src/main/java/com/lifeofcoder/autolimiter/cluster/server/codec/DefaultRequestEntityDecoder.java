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

import com.lifeofcoder.autolimiter.cluster.common.codec.EntityDecoder;
import com.lifeofcoder.autolimiter.cluster.common.codec.request.RequestEntityDecoder;
import com.lifeofcoder.autolimiter.cluster.common.request.ClusterRequest;
import com.lifeofcoder.autolimiter.cluster.server.codec.registry.RequestDataDecodeRegistry;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Default entity decoder for any {@link ClusterRequest} entity.</p>
 *
 * <p>Decode format:</p>
 * <pre>
 * +--------+---------+---------+
 * | xid(4) | type(1) | data... |
 * +--------+---------+---------+
 * </pre>
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
public class DefaultRequestEntityDecoder implements RequestEntityDecoder<ByteBuf, ClusterRequest> {
    private static Logger LOGGER = LoggerFactory.getLogger(DefaultRequestEntityDecoder.class);

    @Override
    public ClusterRequest decode(ByteBuf source) {
        if (source.readableBytes() >= 5) {
            int xid = source.readInt();
            int type = source.readByte();

            EntityDecoder<ByteBuf, ?> dataDecoder = RequestDataDecodeRegistry.getDecoder(type);
            if (dataDecoder == null) {
                LOGGER.warn("Unknown type of request data decoder: {}", type);
                return null;
            }

            Object data;
            if (source.readableBytes() == 0) {
                data = null;
            }
            else {
                data = dataDecoder.decode(source);
            }

            return new ClusterRequest<>(xid, type, data);
        }
        return null;
    }
}
