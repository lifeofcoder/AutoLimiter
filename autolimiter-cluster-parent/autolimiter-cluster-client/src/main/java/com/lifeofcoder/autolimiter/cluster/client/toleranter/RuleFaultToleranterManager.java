package com.lifeofcoder.autolimiter.cluster.client.toleranter;

import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.common.config.ClusterServerConfigItem;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;
import com.lifeofcoder.autolimiter.common.utils.ValidatorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 规则容错管理
 *
 * @author xbc
 * @date 2022/3/10
 */
public class RuleFaultToleranterManager implements FaultToleranterManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(RuleFaultToleranterManager.class);

    /**
     * 资源路由，计数节点维度
     * key: id+port
     */
    private volatile Map<String, DiagnoseFaultToleranter> diagnoseFaultToleranterMap;

    @Override
    public DiagnoseFaultToleranter selectDiagnoseFaultToleranter(String host, int port) {
        Map<String, DiagnoseFaultToleranter> tmpDiagnoseFaultToleranterMap = diagnoseFaultToleranterMap;
        if (ValidatorHelper.isEmpty(tmpDiagnoseFaultToleranterMap)) {
            LOGGER.debug("The DiagnoseFaultToleranterMap is empty.");
            return null;
        }

        return tmpDiagnoseFaultToleranterMap.get(buildHostKey(host, port));
    }

    private static String buildHostKey(String ip, Integer port) {
        return port + ":" + ip;
    }

    @Override
    public synchronized void clusterServerTopoConfigChanged(ClusterServerTopoConfig clusterServerTopoConfig) {
        String hostKey;
        Map<String, DiagnoseFaultToleranter> tmpTolernaterMap = new HashMap<>();
        Map<String, DiagnoseFaultToleranter> oldTolernaterMap = diagnoseFaultToleranterMap;
        if (oldTolernaterMap == null) {
            oldTolernaterMap = new HashMap<>();
        }

        if (null != clusterServerTopoConfig && ValidatorHelper.isNotEmpty(clusterServerTopoConfig.getServers())) {
            DiagnoseFaultToleranter diagnoseFaultToleranter;
            for (ClusterServerConfigItem server : clusterServerTopoConfig.getServers()) {
                hostKey = buildHostKey(server.getIp(), server.getPort());
                diagnoseFaultToleranter = oldTolernaterMap.get(hostKey);
                if (null == diagnoseFaultToleranter) {
                    diagnoseFaultToleranter = new DiagnoseFaultToleranter(server.getIp(), server.getPort(), server.getAvailableRatio(), getAvailableRatio(server.getAvailableRatio()));
                }
                tmpTolernaterMap.put(hostKey, diagnoseFaultToleranter);
            }
        }
        diagnoseFaultToleranterMap = tmpTolernaterMap;
    }

    @Override
    public void availableRatioChanged(Double availableRatio) {
        Map<String, DiagnoseFaultToleranter> tmpTolernaterMap = new HashMap<>();
        if (ValidatorHelper.isEmpty(tmpTolernaterMap)) {
            return;
        }

        for (DiagnoseFaultToleranter toleranter : tmpTolernaterMap.values()) {
            toleranter.updateAvailableRatio(availableRatio);
        }
    }

    private static double getAvailableRatio(Double availableRatio) {
        return ClusterClientConfigManager.getMinAvailableRatio(availableRatio);
    }
}
