package com.lifeofcoder.autolimiter.cluster.web.config;

import com.lifeofcoder.autolimiter.cluster.server.config.ClusterServerConfigModifier;
import com.lifeofcoder.autolimiter.cluster.web.consts.FlowConsts;
import org.springframework.stereotype.Component;

/**
 * 集群控制参数
 *
 * @author xbc
 * @date 2021/7/2
 */
@Component
public class ClusterConfigChangedListener {
    protected void valueChanged(String s) {
        ClusterServerConfigModifier.updateClusterConfig(FlowConsts.DEFAULT_NAMESPACE, s);
    }

    protected String key() {
        return FlowConsts.CLUSTER_CONFIG_KEY;
    }
}
