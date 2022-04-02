package com.lifeofcoder.autolimiter.cluster.client.config;

import com.alibaba.fastjson.JSON;
import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.cluster.client.toleranter.DiagnoseFaultToleranterProxy;
import com.lifeofcoder.autolimiter.common.config.ClusterClientConfig;
import com.lifeofcoder.autolimiter.common.config.ClusterConfigChangedHandler;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 集群限流客户端配置变更处理器(基于SPI加载)
 *
 * @author xbc
 * @date 2020/4/21
 */
public class ClusterClientClusterConfigChangedHandler implements ClusterConfigChangedHandler {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterClientClusterConfigChangedHandler.class);

    public void configChanged(String key, String config) {
        //先更新配置变更
        if (Objects.equals(key, ClusterClientConfig.KEY)) {
            LOGGER.info("The new ClusterClientConfig is " + config);
            ClusterClientConfig clusterClientConfig = JSON.parseObject(config, ClusterClientConfig.class);
            if (null == clusterClientConfig) {
                clusterClientConfig = new ClusterClientConfig();
            }

            //执行客户端变更
            ClusterClientConfigManager.clusterClientConfigChanged(clusterClientConfig);

            //执行容错策略变更
            DiagnoseFaultToleranterProxy.clusterClientConfigChanged(clusterClientConfig);
        }

        //再更新topo变更
        if (Objects.equals(key, ClusterServerTopoConfig.KEY)) {
            LOGGER.info("The new ClusterServerTopoConfig is " + config);
            ClusterServerTopoConfig entity = JSON.parseObject(config, ClusterServerTopoConfig.class);
            //执行客户端变更
            ClusterClientConfigManager.clusterServerTopoChanged(entity);

            //等待客户端都变更了后，容错策略变更
            DiagnoseFaultToleranterProxy.clusterServerTopoChanged(entity);
        }
    }
}
