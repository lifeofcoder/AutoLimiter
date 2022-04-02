package com.lifeofcoder.autolimiter.cluster.client.dynamic;

import com.alibaba.csp.sentinel.cluster.client.ClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.common.result.DynamicTokenResult;

/**
 * 动态集群客户端
 *
 * @author xbc
 * @date 2022/3/17
 */
public interface DynamicClusterTokenClient extends ClusterTokenClient {
    DynamicTokenResult requestDynamicToken(long flowId, int maxCount, int lastCount);
}
