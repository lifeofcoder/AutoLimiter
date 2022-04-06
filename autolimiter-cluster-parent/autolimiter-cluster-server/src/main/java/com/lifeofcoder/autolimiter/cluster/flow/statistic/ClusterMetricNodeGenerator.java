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
package com.lifeofcoder.autolimiter.cluster.flow.statistic;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.util.TimeUtil;
import com.lifeofcoder.autolimiter.cluster.flow.rule.ClusterFlowRuleManager;
import com.lifeofcoder.autolimiter.cluster.flow.rule.ClusterParamFlowRuleManager;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.data.ClusterFlowEvent;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.metric.ClusterMetric;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.metric.ClusterParamMetric;
import com.lifeofcoder.autolimiter.common.utils.ValidatorHelper;

import java.util.*;

/**
 * @author Eric Zhao
 * @since 1.4.1
 */
public class ClusterMetricNodeGenerator {
    public static Map<String, List<ClusterMetricNode>> generateCurrentNodeMap(String namespace, long startTimeMs, long endTimeMs) {
        Map<String, List<ClusterMetricNode>> map = new HashMap<>();
        Set<Long> flowIds = ClusterFlowRuleManager.getFlowIdSet(namespace);
        Set<Long> paramFlowIds = ClusterParamFlowRuleManager.getFlowIdSet(namespace);
        for (Long id : flowIds) {
            List<ClusterMetricNode> nodeList = flowToMetricNodes(id, startTimeMs, endTimeMs);
            if (ValidatorHelper.isEmpty(nodeList)) {
                continue;
            }

            for (ClusterMetricNode node : nodeList) {
                putToMap(map, node);
            }
        }
        for (Long id : paramFlowIds) {
            ClusterMetricNode node = paramToMetricNode(id);
            if (node == null) {
                continue;
            }
            putToMap(map, node);
        }

        return map;
    }

    public static Map<String, List<ClusterMetricNode>> generateCurrentNodeMap(String namespace) {
        Map<String, List<ClusterMetricNode>> map = new HashMap<>();
        Set<Long> flowIds = ClusterFlowRuleManager.getFlowIdSet(namespace);
        Set<Long> paramFlowIds = ClusterParamFlowRuleManager.getFlowIdSet(namespace);
        for (Long id : flowIds) {
            ClusterMetricNode node = flowToMetricNode(id);
            if (node == null) {
                continue;
            }
            putToMap(map, node);
        }
        for (Long id : paramFlowIds) {
            ClusterMetricNode node = paramToMetricNode(id);
            if (node == null) {
                continue;
            }
            putToMap(map, node);
        }

        return map;
    }

    private static void putToMap(Map<String, List<ClusterMetricNode>> map, ClusterMetricNode node) {
        List<ClusterMetricNode> nodeList = map.get(node.getResourceName());
        if (nodeList == null) {
            nodeList = new ArrayList<>();
            map.put(node.getResourceName(), nodeList);
        }
        nodeList.add(node);
    }

    public static List<ClusterMetricNode> flowToMetricNodes(long flowId, long startTime, long endTime) {
        FlowRule rule = ClusterFlowRuleManager.getFlowRuleById(flowId);
        if (rule == null) {
            return null;
        }
        ClusterMetric metric = ClusterMetricStatistics.getMetric(flowId);
        if (metric == null) {
            return null;
        }

        List<ClusterMetricNode> clusterMetricNodeList = new ArrayList<>();
        for (long i = startTime / 1000; i <= endTime / 1000; i++) {
            ClusterMetricNode history = metric.getHistory(i);
            if (null == history) {
                continue;
            }
            clusterMetricNodeList.add(history.setFlowId(flowId).setResourceName(rule.getResource()));
        }

        return clusterMetricNodeList;
    }

    public static ClusterMetricNode flowToMetricNode(long flowId) {
        FlowRule rule = ClusterFlowRuleManager.getFlowRuleById(flowId);
        if (rule == null) {
            return null;
        }
        ClusterMetric metric = ClusterMetricStatistics.getMetric(flowId);
        if (metric == null) {
            return new ClusterMetricNode().setFlowId(flowId).setResourceName(rule.getResource());
        }

        ClusterMetricNode clusterMetricNode = new ClusterMetricNode().setFlowId(flowId).setResourceName(rule.getResource()).setBlockQps(metric.getAvg(ClusterFlowEvent.BLOCK)).setTimestamp(TimeUtil.currentTimeMillis())
                .setPassQps(metric.getAvg(ClusterFlowEvent.PASS));
        return clusterMetricNode;
    }

    public static ClusterMetricNode paramToMetricNode(long flowId) {
        ParamFlowRule rule = ClusterParamFlowRuleManager.getParamRuleById(flowId);
        if (rule == null) {
            return null;
        }
        ClusterParamMetric metric = ClusterParamMetricStatistics.getMetric(flowId);
        if (metric == null) {
            return new ClusterMetricNode().setFlowId(flowId).setResourceName(rule.getResource()).setTimestamp(TimeUtil.currentTimeMillis()).setTopParams(new HashMap<Object, Double>(0));
        }
        return new ClusterMetricNode().setFlowId(flowId).setResourceName(rule.getResource()).setTimestamp(TimeUtil.currentTimeMillis()).setTopParams(metric.getTopValues(5));
    }
}
