package com.lifeofcoder.autolimiter.cluster.client.toleranter;

import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.cluster.client.consts.CommonParams;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;

/**
 * 单节点容错
 *
 * @author xbc
 * @date 2022/3/10
 */
public class SystemFaultToleranterManager implements FaultToleranterManager {

    private volatile DiagnoseFaultToleranter diagnoseFaultToleranter;

    @Override
    public DiagnoseFaultToleranter selectDiagnoseFaultToleranter(String host, int port) {
        return diagnoseFaultToleranter;
    }

    @Override
    public void clusterServerTopoConfigChanged(ClusterServerTopoConfig clusterServerTopoConfig) {
        if (null == clusterServerTopoConfig || !clusterServerTopoConfig.isValid()) {
            diagnoseFaultToleranter = null;
            return;
        }

        if (null == diagnoseFaultToleranter || !diagnoseFaultToleranter.isSameNode(clusterServerTopoConfig.getServerHost(), clusterServerTopoConfig.getServerPort())) {
            diagnoseFaultToleranter = new DiagnoseFaultToleranter(clusterServerTopoConfig.getServerHost(), clusterServerTopoConfig.getServerPort(), CommonParams.DEF_MIN_AVAILABLE_RATIO, getAvailableRatio());
        }
    }

    private double getAvailableRatio() {
        return ClusterClientConfigManager.getGlobalMinAvaiableRatio();
    }

    @Override
    public void availableRatioChanged(Double availableRatio) {
        DiagnoseFaultToleranter tmpDiagnoseFaultToleranter = diagnoseFaultToleranter;
        if (null == tmpDiagnoseFaultToleranter) {
            return;
        }

        tmpDiagnoseFaultToleranter.updateAvailableRatio(availableRatio);
    }
}