package com.lifeofcoder.autolimiter.common.rule;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;

import java.util.Objects;

/**
 * 自定义限流规则
 *
 * @author xbc
 * @date 2021/8/27
 */
public class CustomizedFlowRule extends FlowRule {
    private boolean shouldAlarm = true;

    /**
     * 集群限流阈值，该数据仅仅用于告警，具体限流并不使用。因此不用放到equal和hashcode中
     */
    private double clusterCount;

    /**
     * 是否开启动态集群
     */
    private boolean dynamicCluster;

    public boolean isShouldAlarm() {
        return shouldAlarm;
    }

    public void setShouldAlarm(boolean shouldAlarm) {
        this.shouldAlarm = shouldAlarm;
    }

    public boolean shouldAlarm() {
        return shouldAlarm;
    }

    public double getClusterCount() {
        return clusterCount;
    }

    public void setClusterCount(double clusterCount) {
        this.clusterCount = clusterCount;
    }

    public boolean isDynamicCluster() {
        return dynamicCluster;
    }

    public void setDynamicCluster(boolean dynamicCluster) {
        this.dynamicCluster = dynamicCluster;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        CustomizedFlowRule that = (CustomizedFlowRule) o;
        return shouldAlarm == that.shouldAlarm && dynamicCluster == that.dynamicCluster;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), shouldAlarm, dynamicCluster);
    }
}
