package com.lifeofcoder.autolimiter.cluster.client.config;

/**
 * 集群客户端配置管理
 *
 * @author xbc
 * @date 2022/3/29
 */
public class ClusterClientConfigCenter {
    /**
     * 已经关闭机器流控
     */
    private static volatile boolean closeClusterLimit;

    /**
     * 设置集群限流开关
     */
    public static void setCloseClusterLimit(boolean closeClusterLimit) {
        ClusterClientConfigCenter.closeClusterLimit = closeClusterLimit;
    }

    /**
     * 集群限流关闭
     */
    public static boolean isClusterLimitClosed() {
        return closeClusterLimit;
    }
}
