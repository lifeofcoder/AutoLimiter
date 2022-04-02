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
import com.alibaba.csp.sentinel.command.annotation.CommandMapping;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricStatistics;
import com.lifeofcoder.autolimiter.cluster.server.config.ClusterServerConfigManager;
import com.lifeofcoder.autolimiter.cluster.server.config.ServerFlowConfig;
import com.lifeofcoder.autolimiter.cluster.server.config.ServerTransportConfig;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
@CommandMapping(name = "cluster/server/fetchConfig", desc = "get cluster server config")
public class FetchClusterServerConfigHandler implements CommandHandler<String> {

    @Override
    public CommandResponse<String> handle(CommandRequest request) {
        String namespace = request.getParam("namespace");
        if (StringUtil.isEmpty(namespace)) {
            return globalConfigResult();
        }
        return namespaceConfigResult(namespace);
    }

    private CommandResponse<String> namespaceConfigResult(/*@NonEmpty*/ String namespace) {
        ServerFlowConfig flowConfig = new ServerFlowConfig().setExceedCount(ClusterServerConfigManager.getExceedCount(namespace)).setMaxOccupyRatio(ClusterServerConfigManager.getMaxOccupyRatio(namespace))
                .setIntervalMs(ClusterServerConfigManager.getIntervalMs(namespace)).setMaxAllowedQps(ClusterServerConfigManager.getMaxAllowedQps(namespace)).setSampleCount(ClusterServerConfigManager.getSampleCount(namespace))
                .setRecordHistory(ClusterMetricStatistics.isRecordHistory());
        JSONObject config = new JSONObject().fluentPut("flow", flowConfig);
        return CommandResponse.ofSuccess(config.toJSONString());
    }

    private CommandResponse<String> globalConfigResult() {
        ServerTransportConfig transportConfig = new ServerTransportConfig().setPort(ClusterServerConfigManager.getPort()).setIdleSeconds(ClusterServerConfigManager.getIdleSeconds());
        ServerFlowConfig flowConfig = new ServerFlowConfig().setExceedCount(ClusterServerConfigManager.getExceedCount()).setMaxOccupyRatio(ClusterServerConfigManager.getMaxOccupyRatio()).setMaxAllowedQps(ClusterServerConfigManager.getMaxAllowedQps())
                .setIntervalMs(ClusterServerConfigManager.getIntervalMs()).setSampleCount(ClusterServerConfigManager.getSampleCount()).setRecordHistory(ClusterMetricStatistics.isRecordHistory());
        JSONObject config = new JSONObject().fluentPut("transport", transportConfig).fluentPut("flow", flowConfig).fluentPut("namespaceSet", ClusterServerConfigManager.getNamespaceSet());
        return CommandResponse.ofSuccess(config.toJSONString());
    }
}

