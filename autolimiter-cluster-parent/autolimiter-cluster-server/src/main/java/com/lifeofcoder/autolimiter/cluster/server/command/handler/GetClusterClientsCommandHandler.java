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
import com.alibaba.fastjson.JSON;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricStatistics;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.metric.ClusterMetric;

/**
 * 查询客户端信息
 * @author xbc
 */
@CommandMapping(name = "cluster/getClientsInfo", desc = "get cluster clients info")
public class GetClusterClientsCommandHandler implements CommandHandler<String> {

    @Override
    public CommandResponse<String> handle(CommandRequest request) {
        String ruleIdStr = request.getParam("ruleId");
        if (StringUtil.isEmpty(ruleIdStr)) {
            return CommandResponse.ofFailure(new IllegalArgumentException("failed: ruleId cannot be empty"));
        }

        Long ruleId = Long.valueOf(ruleIdStr);
        if (ruleId == null || ruleId < 1) {
            return CommandResponse.ofFailure(new IllegalArgumentException("failed: ruleId is invalid."));
        }

        ClusterMetric metric = ClusterMetricStatistics.getMetricAndRecordHistory(ruleId);
        if (null == metric) {
            return CommandResponse.ofFailure(new IllegalArgumentException("failed: Can't find ClusterMetric."));
        }

        return CommandResponse.ofSuccess(JSON.toJSONString(metric.getClusterRuleClients().getClientsInfo()));
    }
}
