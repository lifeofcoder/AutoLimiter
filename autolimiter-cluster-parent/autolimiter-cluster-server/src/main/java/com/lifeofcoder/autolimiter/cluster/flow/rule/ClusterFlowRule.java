package com.lifeofcoder.autolimiter.cluster.flow.rule;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;

import java.util.Objects;

/**
 *集群支持的FlowRule
 *
 * @author xbc
 * @date 2021/6/28
 */
public class ClusterFlowRule extends FlowRule {
    private int version;

    /**
     * 集群限流阈值
     */
    private Double clusterCount;

    /**
     * 备份单机限流
     */
    private Double localCount;

    public Double getClusterCount() {
        return clusterCount;
    }

    public void setClusterCount(Double clusterCount) {
        this.clusterCount = clusterCount;
    }

    @Override
    public boolean equals(Object o) {
        boolean equqls = super.equals(o);
        //不相等，就肯定不相等
        if (!equqls) {
            return equqls;
        }

        //如果父类相等，还需要判断子类是否相等
        if (!(o instanceof ClusterFlowRule)) {
            return false;
        }

        ClusterFlowRule clusterFlowRule = (ClusterFlowRule) o;
        return Objects.equals(clusterFlowRule.getClusterCount(), this.clusterCount);
    }

    /**
     * 切换为集群模式，将clusterCount的值赋值给count。因为底层都是用count字段
     */
    public ClusterFlowRule toClusterMode() {
        double count = getCount();
        setCount(getClusterCount());
        setLocalCount(count);
        return this;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Double getLocalCount() {
        return localCount;
    }

    public void setLocalCount(Double localCount) {
        this.localCount = localCount;
    }
}
