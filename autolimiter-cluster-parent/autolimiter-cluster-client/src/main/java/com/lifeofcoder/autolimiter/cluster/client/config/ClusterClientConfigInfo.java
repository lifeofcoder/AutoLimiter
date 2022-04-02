package com.lifeofcoder.autolimiter.cluster.client.config;

import java.io.Serializable;

/**
 *
 *
 * @author xbc
 * @date 2022/3/29
 */
public class ClusterClientConfigInfo implements Serializable {

    private static final long serialVersionUID = -5465994303376042691L;

    /**
     * 关闭集群限流
     */
    private boolean closeCluster;

    public boolean isCloseCluster() {
        return closeCluster;
    }

    public void setCloseCluster(boolean closeCluster) {
        this.closeCluster = closeCluster;
    }
}
