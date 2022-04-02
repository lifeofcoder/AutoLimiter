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
package com.lifeofcoder.autolimiter.cluster.server.command.handler;

import com.alibaba.csp.sentinel.command.CommandHandler;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.lifeofcoder.autolimiter.cluster.server.config.ClusterServerConfigModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
//@CommandMapping(name = "cluster/server/modifyFlowConfig", desc = "modify cluster server flow config")
public class ModifyClusterServerFlowConfigHandler implements CommandHandler<String> {
    private final static Logger LOGGER = LoggerFactory.getLogger(ModifyClusterServerFlowConfigHandler.class);

    @Override
    public CommandResponse<String> handle(CommandRequest request) {
        String data = request.getParam("data");
        if (StringUtil.isBlank(data)) {
            return CommandResponse.ofFailure(new IllegalArgumentException("empty data"));
        }
        String namespace = request.getParam("namespace");
        try {
            data = URLDecoder.decode(data, "utf-8");
            return ClusterServerConfigModifier.updateClusterConfig(namespace, data);
        }
        catch (Exception e) {
            LOGGER.warn("[ModifyClusterServerFlowConfigHandler] Decode cluster server flow config error", e);
            return CommandResponse.ofFailure(e, "decode cluster server flow config error");
        }
    }
}
