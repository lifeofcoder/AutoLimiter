package com.lifeofcoder.autolimiter.cluster.client;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenServerDescriptor;
import com.alibaba.csp.sentinel.cluster.client.ClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.client.dynamic.DynamicClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.command.entity.ClusterClientStateEntity;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;

import java.util.Collection;
import java.util.List;

/**
 * 可变的客户端,不提供TokenServer的能力。考虑到很多底层都加载的ClusterTokenClient。所以只能屏蔽掉TokenServer的功能
 *
 * @author xbc
 * @date 2021/7/22
 */
public interface ChangeableClusterTokenClient extends ClusterTokenClient {
    @Override
    default TokenServerDescriptor currentServer() {
        return null;
    }

    @Override
    default TokenResult requestParamToken(Long ruleId, int acquireCount, Collection<Object> params) {
        throw new RuntimeException("Method is not supported.");
    }

    @Override
    default TokenResult requestToken(Long ruleId, int acquireCount, boolean prioritized) {
        throw new RuntimeException("Method is not supported.");
    }

    /**
     * 集群服务端Topo结构变更
     * @param topoConfig 为空表示没有节点可用，此时整个计数集群都不可用
     */
    void clusterServerTopoChanged(ClusterServerTopoConfig topoConfig);

    /**
     * 查询当前连接的服务端信息
     */
    List<ClusterClientStateEntity> listClusterClientState();

    /**
     * 根据流控资源ID选择集群限流客户端
     */
    DynamicClusterTokenClient selectClusterTokenClient(long flowId);
}