package com.lifeofcoder.autolimiter.cluster.client.toleranter;

import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;

/**
 * 容错策略管理
 *
 * @author xbc
 * @date 2022/3/10
 */
public interface FaultToleranterManager {
    /**
     * 选择容错策略
     */
    DiagnoseFaultToleranter selectDiagnoseFaultToleranter(String host, int port);

    /**
     * Topo结构变更
     */
    void clusterServerTopoConfigChanged(ClusterServerTopoConfig clusterServerTopoConfig);

    /**
     * 可用率变更
     */
    void availableRatioChanged(Double availableRatio);
}