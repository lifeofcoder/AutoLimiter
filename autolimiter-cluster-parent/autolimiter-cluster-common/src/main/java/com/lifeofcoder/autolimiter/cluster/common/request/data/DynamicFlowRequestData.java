package com.lifeofcoder.autolimiter.cluster.common.request.data;

/**
 * 动态流控请求数据
 *
 * @author xbc
 * @date 2022/3/17
 */
public class DynamicFlowRequestData {
    /**
     * 客户端IP
     */
    private long ip;

    /**
     * 资源ID
     */
    private long flowId;

    /**
     * 节点当前承接的集群限流数
     */
    private int maxCount;

    /**
     * 过去一秒的请求数
     */
    private int lastCount;

    public long getIp() {
        return ip;
    }

    public DynamicFlowRequestData setIp(long ip) {
        this.ip = ip;
        return this;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public DynamicFlowRequestData setMaxCount(int maxCount) {
        this.maxCount = maxCount;
        return this;
    }

    public long getFlowId() {
        return flowId;
    }

    public DynamicFlowRequestData setFlowId(long flowId) {
        this.flowId = flowId;
        return this;
    }

    public int getLastCount() {
        return lastCount;
    }

    public DynamicFlowRequestData setLastCount(int lastCount) {
        this.lastCount = lastCount;
        return this;
    }
}
