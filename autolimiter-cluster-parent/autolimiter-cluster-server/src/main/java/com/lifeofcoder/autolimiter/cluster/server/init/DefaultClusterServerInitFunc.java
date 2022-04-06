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
package com.lifeofcoder.autolimiter.cluster.server.init;

import com.alibaba.csp.sentinel.init.InitFunc;
import com.lifeofcoder.autolimiter.cluster.common.ClusterConstants;
import com.lifeofcoder.autolimiter.cluster.server.TokenServiceProvider;
import com.lifeofcoder.autolimiter.cluster.server.codec.data.*;
import com.lifeofcoder.autolimiter.cluster.server.codec.registry.RequestDataDecodeRegistry;
import com.lifeofcoder.autolimiter.cluster.server.codec.registry.ResponseDataWriterRegistry;
import com.lifeofcoder.autolimiter.cluster.server.processor.RequestProcessorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class DefaultClusterServerInitFunc implements InitFunc {
    private static Logger LOGGER = LoggerFactory.getLogger(DefaultClusterServerInitFunc.class);

    @Override
    public void init() throws Exception {
        initDefaultEntityDecoders();
        initDefaultEntityWriters();

        initDefaultProcessors();

        // Eagerly-trigger the SPI pre-load of token service.
        TokenServiceProvider.getService();

        LOGGER.info("[DefaultClusterServerInitFunc] Default entity codec and processors registered");
    }

    private void initDefaultEntityWriters() {
        ResponseDataWriterRegistry.addWriter(ClusterConstants.MSG_TYPE_PING, new PingResponseDataWriter());
        ResponseDataWriterRegistry.addWriter(ClusterConstants.MSG_TYPE_FLOW, new FlowResponseDataWriter());
        ResponseDataWriterRegistry.addWriter(ClusterConstants.MSG_TYPE_PARAM_FLOW, new FlowResponseDataWriter());
        ResponseDataWriterRegistry.addWriter(ClusterConstants.MSG_TYPE_DYNAMIC_FLOW, new DynamicFlowResponseDataWriter());
    }

    private void initDefaultEntityDecoders() {
        RequestDataDecodeRegistry.addDecoder(ClusterConstants.MSG_TYPE_PING, new PingRequestDataDecoder());
        RequestDataDecodeRegistry.addDecoder(ClusterConstants.MSG_TYPE_FLOW, new FlowRequestDataDecoder());
        RequestDataDecodeRegistry.addDecoder(ClusterConstants.MSG_TYPE_PARAM_FLOW, new ParamFlowRequestDataDecoder());
        RequestDataDecodeRegistry.addDecoder(ClusterConstants.MSG_TYPE_DYNAMIC_FLOW, new DynamicFlowRequestDataDecoder());
    }

    private void initDefaultProcessors() {
        // Eagerly-trigger the SPI pre-load.
        RequestProcessorProvider.getProcessor(0);
    }
}
