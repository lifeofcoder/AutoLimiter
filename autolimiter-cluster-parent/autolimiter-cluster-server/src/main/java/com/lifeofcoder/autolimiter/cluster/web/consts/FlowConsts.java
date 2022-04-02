package com.lifeofcoder.autolimiter.cluster.web.consts;

/**
 * 流控相关
 *
 * @author xbc
 * @date 2021/6/10
 */
public class FlowConsts {
    public static final String DEFAULT_NAMESPACE = "default";

    public static final String CLUSTER_CONFIG_KEY = "cluster_flow_config";

    public static final String SERVER_TRANSPORT_CONFIG = "server_transport_config";

    /**
     * 计数配置
     */
    public static final String COUNTER_SERVER_CONFIG = "counter_server_config";

    public static final String METRIC_TYPE_UPDATE = "update";

    public static final float DEF_CLIENT_COUNT_CHANGE_RATIO = 0.05F;
}
