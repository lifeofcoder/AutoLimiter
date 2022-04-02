package com.lifeofcoder.autolimiter.cluster.client.rule;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.lifeofcoder.autolimiter.cluster.client.consts.ClusterCheckResult;
import com.lifeofcoder.autolimiter.cluster.client.dynamic.DynamicFlowController;

/**
 * 动态集群规则检测
 */
public class DynamicClusterFlowRuleChecker extends AbstractClusterFlowRuleChecker {
    /**
     * 动态流控控制器
     */
    private DynamicFlowController dynamicFlowController = new DynamicFlowController();

    @Override
    protected ClusterCheckResult passClusterCheck0(FlowRule rule, Context context, DefaultNode node, int acquireCount, boolean prioritized) {
        return dynamicFlowController.clusterFlowCheck(rule.getClusterConfig().getFlowId());
    }
}