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
package com.lifeofcoder.autolimiter.cluster.client.codec.data;

import com.lifeofcoder.autolimiter.cluster.common.codec.EntityDecoder;
import com.lifeofcoder.autolimiter.cluster.common.response.data.DynamicFlowTokenResponseData;
import io.netty.buffer.ByteBuf;

/**
 * 动态流控响应解码器
 * @author xbc
 */
public class DynamicFlowResponseDataDecoder implements EntityDecoder<ByteBuf, DynamicFlowTokenResponseData> {

    @Override
    public DynamicFlowTokenResponseData decode(ByteBuf source) {
        DynamicFlowTokenResponseData data = new DynamicFlowTokenResponseData();

        if (source.readableBytes() == 8) {
            data.setCount(source.readInt());
            data.setWaitInMs(source.readInt());
        }
        return data;
    }
}
