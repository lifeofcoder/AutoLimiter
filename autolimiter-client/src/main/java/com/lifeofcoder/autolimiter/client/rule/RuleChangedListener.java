package com.lifeofcoder.autolimiter.client.rule;

import com.alibaba.csp.sentinel.slots.block.Rule;

import java.util.List;

/**
 * 规则变更监听
 *
 * @author xbc
 * @date 2022/3/17
 */
public interface RuleChangedListener {
    void ruleChanged(List<? extends Rule> newRuleList);
}
