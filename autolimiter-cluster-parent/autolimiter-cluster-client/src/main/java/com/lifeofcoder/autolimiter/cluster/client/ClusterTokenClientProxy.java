package com.lifeofcoder.autolimiter.cluster.client;

import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.cluster.client.common.config.ServerChangeObserver;
import com.lifeofcoder.autolimiter.cluster.client.consts.CommonParams;
import com.lifeofcoder.autolimiter.cluster.client.dynamic.DynamicClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.command.entity.ClusterClientStateEntity;
import com.lifeofcoder.autolimiter.common.config.ClusterClientConfig;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;
import com.lifeofcoder.autolimiter.common.model.RouteMode;
import com.lifeofcoder.autolimiter.common.utils.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 代理客户端，支持根据不同的配置选择不同的类型的客户端
 *
 * @author xbc
 * @date 2021/7/22
 */
public class ClusterTokenClientProxy implements ChangeableClusterTokenClient {
    private static Logger LOGGER = LoggerFactory.getLogger(ClusterTokenClientProxy.class);

    private volatile ChangeableClusterTokenClient delegateClient;

    private volatile ClusterClientConfig clusterClientConfig = new ClusterClientConfig();

    public ClusterTokenClientProxy() {
        ClusterClientConfigManager.addServerChangeObserver(new ServerChangeObserver() {
            @Override
            public void onClusterServerTopoChange(ClusterServerTopoConfig assignConfig) {
                clusterServerTopoChanged(assignConfig);
            }

            @Override
            public void onClusterClientConfigChange(ClusterClientConfig clusterClientConfig) {
                clusterClientConfigChanged(clusterClientConfig);
            }
        });

        //必须要这样处理，因为ProxyClusterTokenClient是基于SPI懒加载的，即没有集群请求就不会初始该类
        //这样在没有初始化的时候配置以及下发了，但是此时没有注册ServerChangeObserver，无法获取到配置数据
        //所以将配置数据每次都写入ClusterClientConfigManager的静态变量，初始化ProxyClusterTokenClient的时候直接读取
        clusterClientConfigChanged(ClusterClientConfigManager.currentClusterClientConfig());
    }

    /**
     * 集群客户端配置变更,负责路由切换
     */
    private synchronized void clusterClientConfigChanged(ClusterClientConfig newClusterClientConfig) {
        try {
            LOGGER.info("A new ClusterClientConfig is received." + newClusterClientConfig);

            //判断路由模式是否变更
            RouteMode currentRouteMode = clusterClientConfig.getRouteMode();
            RouteMode newRouteMode = newClusterClientConfig.getRouteMode();
            //路由模式改变，关闭之前的客户端，启动新的客户端
            if (delegateClient == null || !Objects.equals(currentRouteMode, newRouteMode)) {
                //新建客户端并启动
                ChangeableClusterTokenClient newClient = getClient(newRouteMode);
                newClient.clusterServerTopoChanged(ClusterClientConfigManager.currentClusterServerTopoConfig());
                newClient.start();

                //将新客户端投入使用
                final ChangeableClusterTokenClient oldClient = delegateClient;
                delegateClient = newClient;

                if (oldClient != null) {
                    //异步关闭，等待所有链接都返回
                    ExecutorHelper.getScheduledExecutorService().schedule(() -> {
                        //最后关闭就客户端
                        try {
                            oldClient.stop();
                            LOGGER.info("Succeeced to close client[" + oldClient.currentServer().toString() + "].");
                        }
                        catch (Exception e) {
                            LOGGER.error("Failed to stop cluster token client[" + oldClient.currentServer().toString() + "].", e);
                        }
                    }, CommonParams.CLOSE_CLIENT_DELAY_SEC, TimeUnit.SECONDS);
                }

            }
            this.clusterClientConfig = newClusterClientConfig;

            LOGGER.info("[ProxyClusterTokenClient] ClusterClientConfig has been changed for client[" + delegateClient.getClass().getSimpleName() + "] : " + newClusterClientConfig);
        }
        catch (Exception ex) {
            LOGGER.error("[ProxyClusterTokenClient] Failed to change ClusterClientConfig", ex);
        }
    }

    @Override
    public synchronized void clusterServerTopoChanged(ClusterServerTopoConfig topoConfig) {
        try {
            LOGGER.info("A new ClusterServerTopoConfig is received." + topoConfig);

            //通知客户端配置发生变更
            if (delegateClient != null) {
                delegateClient.clusterServerTopoChanged(topoConfig);
            }
            else {
                LOGGER.info("There is no need up date clusterServerTopoChanged because of no DelegateClient.");
            }

            LOGGER.info("[ProxyClusterTokenClient] ClusterServerTopoConfig has been changed for client[" + delegateClient.getClass().getSimpleName() + "] : " + topoConfig);
        }
        catch (Exception ex) {
            LOGGER.error("[ProxyClusterTokenClient] Failed to change ClusterServerTopoConfig", ex);
        }
    }

    private ChangeableClusterTokenClient getClient(RouteMode routeMode) {
        return RouteMode.RULE == routeMode ? new MappedClusterTokenClient() : new SystemClusterTokenClient();
    }

    @Override
    public void start() throws Exception {
        ChangeableClusterTokenClient client = delegateClient;
        if (client != null) {
            client.start();
        }
    }

    @Override
    public void stop() throws Exception {
        ChangeableClusterTokenClient client = delegateClient;
        if (client != null) {
            client.stop();
        }
    }

    @Override
    public int getState() {
        ChangeableClusterTokenClient client = delegateClient;
        if (null != client) {
            return client.getState();
        }

        return ClientConstants.CLIENT_STATUS_OFF;
    }

    @Override
    public DynamicClusterTokenClient selectClusterTokenClient(long flowId) {
        ChangeableClusterTokenClient client = delegateClient;
        if (null == client) {
            return null;
        }

        return client.selectClusterTokenClient(flowId);
    }

    @Override
    public List<ClusterClientStateEntity> listClusterClientState() {
        return delegateClient.listClusterClientState();
    }
}