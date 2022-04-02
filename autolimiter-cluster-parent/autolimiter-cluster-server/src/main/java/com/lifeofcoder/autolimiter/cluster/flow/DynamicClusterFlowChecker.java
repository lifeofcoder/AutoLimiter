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
package com.lifeofcoder.autolimiter.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.slots.block.ClusterRuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.fastjson.JSON;
import com.lifeofcoder.autolimiter.cluster.common.request.data.DynamicFlowRequestData;
import com.lifeofcoder.autolimiter.cluster.common.result.DynamicTokenResult;
import com.lifeofcoder.autolimiter.cluster.flow.client.ClusterClientInfo;
import com.lifeofcoder.autolimiter.cluster.flow.rule.ClusterFlowRuleManager;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricStatistics;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.data.ClusterFlowEvent;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.limit.GlobalRequestLimiter;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.metric.ClusterMetric;
import com.lifeofcoder.autolimiter.cluster.web.consts.FlowConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flow checker for cluster flow rules.
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
public final class DynamicClusterFlowChecker {
    private static Logger LOGGER = LoggerFactory.getLogger(DynamicClusterFlowChecker.class);

    /**
     * 最小变更率，减少客户端轻微波动
     */
    private static float MIN_CHANGE_RATIO = FlowConsts.DEF_CLIENT_COUNT_CHANGE_RATIO;

    private static double calcGlobalThreshold(FlowRule rule) {
        double count = rule.getCount();
        switch (rule.getClusterConfig().getThresholdType()) {
            case ClusterRuleConstant.FLOW_THRESHOLD_GLOBAL:
                return count;
            case ClusterRuleConstant.FLOW_THRESHOLD_AVG_LOCAL:
            default:
                int connectedCount = ClusterFlowRuleManager.getConnectedCount(rule.getClusterConfig().getFlowId());
                return count * connectedCount;
        }
    }

    public static void setMinChangeRatio(float minChangeRatio) {
        if (minChangeRatio > 1 || minChangeRatio < 0) {
            MIN_CHANGE_RATIO = FlowConsts.DEF_CLIENT_COUNT_CHANGE_RATIO;
            return;
        }

        MIN_CHANGE_RATIO = minChangeRatio;
    }

    static boolean allowProceed(long flowId) {
        String namespace = ClusterFlowRuleManager.getNamespace(flowId);
        return GlobalRequestLimiter.tryPass(namespace);
    }

    static DynamicTokenResult acquireClusterToken(/*@Valid*/ FlowRule rule, DynamicFlowRequestData requestData) {
        Long ruleId = rule.getClusterConfig().getFlowId();
        int acquireCount = 1;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The request of acquireClusterToken for [" + ruleId + "] is " + JSON.toJSONString(requestData));
        }

        if (null == requestData) {
            return new DynamicTokenResult(TokenResultStatus.FAIL);
        }

        ClusterMetric metric = ClusterMetricStatistics.getMetricAndRecordHistory(ruleId);
        if (metric == null) {
            return new DynamicTokenResult(TokenResultStatus.FAIL);
        }

        //判断是否超过集群集群单机最大限流，包含集群Counter机器
        if (!allowProceed(ruleId)) {
            metric.add(ClusterFlowEvent.BLOCK, acquireCount);
            metric.add(ClusterFlowEvent.BLOCK_REQUEST, 1);
            return new DynamicTokenResult(TokenResultStatus.TOO_MANY_REQUEST);
        }

        //将新来的数据计入统计数据
        metric.add(ClusterFlowEvent.PASS, requestData.getLastCount());
        metric.add(ClusterFlowEvent.PASS_REQUEST, 1);

        /**
         * 计算最新的限流值
         */
        double latestQps = metric.getAvg(ClusterFlowEvent.PASS);
        double globalCount = calcGlobalThreshold(rule);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The global info of [" + ruleId + "] is [latestQps:" + latestQps + ", globalCount: " + globalCount + ", remain:" + metric.getRemain() + "].");
        }

        //计算最新占比,得到最新限流值
        double flowRatio = requestData.getLastCount() / latestQps;
        int calNewMaxCount = (int) (globalCount * flowRatio);
        int left = requestData.getMaxCount() - calNewMaxCount;

        //计算变更率
        if (requestData.getMaxCount() > 0) {
            float changeRatio = ((float) Math.abs(left)) / requestData.getMaxCount();
            if (changeRatio < MIN_CHANGE_RATIO) {
                //变更过小，不进行变更
                calNewMaxCount = requestData.getMaxCount();
            }
        }

        ClusterClientInfo clusterClientNode = metric.getClusterRuleClients().touch(requestData.getIp());

        //获取到最新的maxCount(并发接口)
        int newMaxCount = clusterClientNode.achieveCount(calNewMaxCount, requestData.getMaxCount());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The new count of [" + ruleId + "] is " + newMaxCount);
        }

        //分配成功
        return new DynamicTokenResult(TokenResultStatus.OK).setCount(newMaxCount).setWaitInMs(0);
    }

    private DynamicClusterFlowChecker() {
    }
}
