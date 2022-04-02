package com.lifeofcoder.autolimiter.cluster.flow.rule;

import java.io.Serializable;

/**
 * 集群限流规则
 *
 * @author xbc
 * @date 2021/6/30
 */
public class ClusterFlowRuleWrapper implements Serializable {
    private Long ruleId;

    /**
     * 如果没有flowRule，则表示删除
     */
    private ClusterFlowRule flowRule;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public ClusterFlowRule getFlowRule() {
        return flowRule;
    }

    public void setFlowRule(ClusterFlowRule flowRule) {
        this.flowRule = flowRule;
    }
}