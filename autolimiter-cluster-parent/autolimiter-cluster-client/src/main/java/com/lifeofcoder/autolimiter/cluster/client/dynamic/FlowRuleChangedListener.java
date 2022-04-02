package com.lifeofcoder.autolimiter.cluster.client.dynamic;

import com.alibaba.csp.sentinel.slots.block.Rule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.lifeofcoder.autolimiter.client.rule.RuleChangedListener;
import com.lifeofcoder.autolimiter.common.rule.CustomizedFlowRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 流控规则变更监听
 *
 * @author xbc
 * @date 2022/3/17
 */
public class FlowRuleChangedListener implements RuleChangedListener {
    public void ruleChanged(List<? extends Rule> newRuleList) {
        List<FlowRule> clusterFlowRuleList = listDynamicClusterRule(newRuleList);
        DynamicFlowCounterManager.ruleChanged(clusterFlowRuleList);
    }

    private List<FlowRule> listDynamicClusterRule(List<? extends Rule> newRuleList) {
        List<FlowRule> clusterFlowRuleList = new ArrayList<>();
        for (Rule rule : newRuleList) {
            if (rule instanceof CustomizedFlowRule) {
                CustomizedFlowRule flowRule = (CustomizedFlowRule) rule;
                //只处理开启了动态集群流控的资源
                if (flowRule.isClusterMode() && flowRule.isDynamicCluster()) {
                    clusterFlowRuleList.add(flowRule);
                }
            }
        }

        return clusterFlowRuleList;
    }
}
