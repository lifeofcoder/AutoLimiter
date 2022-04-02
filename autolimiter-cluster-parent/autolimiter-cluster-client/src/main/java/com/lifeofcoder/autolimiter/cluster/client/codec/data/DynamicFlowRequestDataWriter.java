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

import com.lifeofcoder.autolimiter.cluster.common.codec.EntityWriter;
import com.lifeofcoder.autolimiter.cluster.common.request.data.DynamicFlowRequestData;
import io.netty.buffer.ByteBuf;

/**
 * 动态流控请求序列化(整体结构是Head+Data, 前面9个字节是head的)
 * +-------------------+--------------+----------------+------------------+-------------------+------------+--------------+
 * | RequestID(8 byte) | Type(1 byte) | FlowID(8 byte) | MaxCount(4 byte) | LastCount(4 byte) | IP(8 byte) | Port(4 byte) |
 * +-------------------+--------------+----------------+------------------+-------------------+------------+--------------+
 *
 * @author xbc
 */
public class DynamicFlowRequestDataWriter implements EntityWriter<DynamicFlowRequestData, ByteBuf> {

    @Override
    public void writeTo(DynamicFlowRequestData entity, ByteBuf target) {
        target.writeLong(entity.getFlowId());
        target.writeInt(entity.getMaxCount());
        target.writeInt(entity.getLastCount());
        target.writeLong(entity.getIp());
    }
}
