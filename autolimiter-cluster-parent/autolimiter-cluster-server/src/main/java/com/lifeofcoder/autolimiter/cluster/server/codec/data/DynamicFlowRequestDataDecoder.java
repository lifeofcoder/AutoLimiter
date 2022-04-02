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
package com.lifeofcoder.autolimiter.cluster.server.codec.data;

import com.lifeofcoder.autolimiter.cluster.common.codec.EntityDecoder;
import com.lifeofcoder.autolimiter.cluster.common.request.data.DynamicFlowRequestData;
import io.netty.buffer.ByteBuf;

/**
 * <p>
 * Decoder for {@link DynamicFlowRequestData} from {@code ByteBuf} stream. The layout:
 * </p>
 * <pre>
 * | flow ID (8) | maxCount (4) | lastCount (4) | ip (8) |
 * </pre>
 *
 * @author xbc
 */
public class DynamicFlowRequestDataDecoder implements EntityDecoder<ByteBuf, DynamicFlowRequestData> {
    @Override
    public DynamicFlowRequestData decode(ByteBuf source) {
        if (source.readableBytes() >= 24) {
            DynamicFlowRequestData requestData = new DynamicFlowRequestData().setFlowId(source.readLong()).setMaxCount(source.readInt()).setLastCount(source.readInt()).setIp(source.readLong());
            return requestData;
        }
        return null;
    }
}