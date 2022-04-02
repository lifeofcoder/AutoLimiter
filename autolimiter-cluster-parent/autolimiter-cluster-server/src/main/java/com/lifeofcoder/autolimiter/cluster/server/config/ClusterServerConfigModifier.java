package com.lifeofcoder.autolimiter.cluster.server.config;

import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricStatistics;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.limit.GlobalRequestLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群配置修改器
 *
 * @author xbc
 * @date 2021/7/2
 */
public class ClusterServerConfigModifier {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterServerConfigModifier.class);

    /**
     * 修改集群配置
     */
    public static CommandResponse<String> updateClusterConfig(String namespace, String configJson) {
        LOGGER.info("[ClusterServerConfigModifier] Receiving cluster server flow config. Namespace : " + namespace + " ,config : " + configJson);
        ServerFlowConfig config = JSON.parseObject(configJson, ServerFlowConfig.class);
        if (StringUtil.isEmpty(namespace)) {
            if (!ClusterServerConfigManager.isValidFlowConfig(config)) {
                return CommandResponse.ofFailure(new IllegalArgumentException("Bad flow config"));
            }
            ClusterServerConfigManager.loadGlobalFlowConfig(config);
        }
        else {
            if (!ClusterServerConfigManager.isValidFlowConfig(config)) {
                return CommandResponse.ofFailure(new IllegalArgumentException("Bad flow config"));
            }
            ClusterServerConfigManager.loadFlowConfig(namespace, config);
        }
        GlobalRequestLimiter.update(namespace, ClusterServerConfigManager.getMaxAllowedQps());
        ClusterMetricStatistics.setRecordHistory(config.isRecordHistory());
        return CommandResponse.ofSuccess("success");
    }
}