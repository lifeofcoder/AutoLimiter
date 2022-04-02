package com.lifeofcoder.autolimiter.cluster.client.rule;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleChecker;
import com.lifeofcoder.autolimiter.common.rule.CustomizedFlowRule;

/**
 * 集群规则检测代理类
 */
public class ClusterFlowRuleCheckerProxy extends FlowRuleChecker {
    private ClusterFlowRuleChecker clusterFlowRuleChecker;
    private DynamicClusterFlowRuleChecker dynamicClusterFlowRuleChecker;

    public ClusterFlowRuleCheckerProxy() {
        clusterFlowRuleChecker = new ClusterFlowRuleChecker();
        dynamicClusterFlowRuleChecker = new DynamicClusterFlowRuleChecker();
    }

    @Override
    public boolean canPassCheck(FlowRule rule, Context context, DefaultNode node, int acquireCount, boolean prioritized) {
        if (rule instanceof CustomizedFlowRule && ((CustomizedFlowRule) rule).isDynamicCluster()) {
            return dynamicClusterFlowRuleChecker.canPassCheck(rule, context, node, acquireCount, prioritized);
        }
        else {
            return clusterFlowRuleChecker.canPassCheck(rule, context, node, acquireCount, prioritized);
        }
    }
}