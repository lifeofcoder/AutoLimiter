package com.lifeofcoder.autolimiter.cluster.common.result;

/**
 * 动态请求结果
 *
 * @author xbc
 * @date 2022/3/17
 */
public class DynamicTokenResult {
    private Integer status;

    /**
     * 最新的流控数据
     */
    private int count;

    /**
     * 等待多久之后再次请求(单位毫秒)
     */
    private int waitInMs;

    public int getCount() {
        return count;
    }

    public DynamicTokenResult setCount(int count) {
        this.count = count;
        return this;
    }

    public int getWaitInMs() {
        return waitInMs;
    }

    public DynamicTokenResult setWaitInMs(int waitInMs) {
        this.waitInMs = waitInMs;
        return this;
    }

    public DynamicTokenResult(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

    public DynamicTokenResult setStatus(Integer status) {
        this.status = status;
        return this;
    }
}
