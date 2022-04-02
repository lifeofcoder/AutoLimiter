/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lifeofcoder.autolimiter.cluster.client;

import com.alibaba.csp.sentinel.cluster.client.ClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.cluster.client.consts.CommonParams;
import com.lifeofcoder.autolimiter.cluster.client.dynamic.DynamicClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.command.entity.ClusterClientStateEntity;
import com.lifeofcoder.autolimiter.common.config.ClusterServerConfigItem;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;
import com.lifeofcoder.autolimiter.common.consistent.ConsistentHashing;
import com.lifeofcoder.autolimiter.common.utils.ExecutorHelper;
import com.lifeofcoder.autolimiter.common.utils.ValidatorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Token客户端映射，支持不同的资源访问不同的服务器（基于一致性Hashing实现）
 * 资源路由
 */
public class MappedClusterTokenClient implements ChangeableClusterTokenClient {
    private static Logger LOGGER = LoggerFactory.getLogger(MappedClusterTokenClient.class);

    private static final int VIRTUAL_NODE_SIZE = 1024;

    private volatile Map<Long, DynamicClusterTokenClient> clusterTokenClientMap = new ConcurrentHashMap<>();

    private volatile List<ClusterTokenServiceClient> clusterTokenClientList = new ArrayList<>();

    private volatile ClusterServerTopoConfig clientAssignConfig;

    private volatile ConsistentHashing<ClusterTokenServiceClient> consistentHashing;

    //这个字段表示的意思是，系统是否需要启动。而不是能不能启动
    private final AtomicBoolean shouldStart = new AtomicBoolean(false);

    public MappedClusterTokenClient() {
        //        ClusterClientConfigManager.addServerChangeObserver(new ServerChangeObserver() {
        //            @Override
        //            public void onRemoteServerChange(ClusterClientAssignConfig assignConfig) {
        //                changeServer(assignConfig);
        //            }
        //        });
        //基于配置加载，初始化的时候不加载客户端
        //        initNewConnection();
    }

    @Override
    public synchronized void clusterServerTopoChanged(ClusterServerTopoConfig topoConfig) {
        if (Objects.equals(clientAssignConfig, topoConfig)) {
            LOGGER.info("Cluster client assisign config didn't change.");
            return;
        }
        try {
            this.clientAssignConfig = topoConfig;

            updateClients();

            //            startClientIfScheduled();
            LOGGER.info("[MappedClusterTokenClient] New pooled client started: " + topoConfig);
        }
        catch (Exception ex) {
            LOGGER.warn("[MappedClusterTokenClient] Failed to change remote token server", ex);
        }
    }

    /**
     * 初始化客户端
     */
    private synchronized void updateClients() {
        List<ClusterServerConfigItem> newServers = null == clientAssignConfig ? null : clientAssignConfig.getServers();
        final List<ClusterTokenServiceClient> toDeleteServerList = new ArrayList<>();
        //所有节点不可用
        if (ValidatorHelper.isEmpty(newServers)) {
            if (clusterTokenClientList != null) {
                toDeleteServerList.addAll(clusterTokenClientList);
            }
            consistentHashing = null;
            clusterTokenClientList = null;
            clusterTokenClientMap = null;
            LOGGER.info("All cluster nodes have been invalid.");
        }
        else {
            //转换为Set方便判断
            Map<String, ClusterServerConfigItem> newServerConfigMap = new HashMap<>(newServers.size());
            for (ClusterServerConfigItem newServerCfg : newServers) {
                newServerConfigMap.put(hostKey(newServerCfg.getIp(), newServerCfg.getPort()), newServerCfg);
            }

            List<ClusterTokenServiceClient> newClusterTokenClientList = new ArrayList<>();
            List<ClusterTokenServiceClient> oldClusterTokenClientList = clusterTokenClientList;
            if (null == oldClusterTokenClientList) {
                oldClusterTokenClientList = new ArrayList<>();
            }

            String oldHostKey;
            for (ClusterTokenServiceClient oldClusterTokenClient : oldClusterTokenClientList) {
                //存在的
                oldHostKey = hostKey(oldClusterTokenClient.getIp(), oldClusterTokenClient.getPort());
                if (newServerConfigMap.containsKey(oldHostKey)) {
                    newClusterTokenClientList.add(oldClusterTokenClient);
                    newServerConfigMap.remove(oldHostKey);
                }
                //被删除了
                else {
                    toDeleteServerList.add(oldClusterTokenClient);
                }
            }

            //说明新的机器列表和已有的机器列表相同
            if (ValidatorHelper.isEmpty(toDeleteServerList) && ValidatorHelper.isEmpty(newServerConfigMap)) {
                LOGGER.info("The counter clusters have not changed.");
                return;
            }

            //newServerConfigMap中剩下的这次新增的服务端
            ClusterTokenServiceClient tmpClient;
            for (ClusterServerConfigItem newServerConfig : newServerConfigMap.values()) {
                tmpClient = new ClusterTokenServiceClient(newServerConfig.getIp(), newServerConfig.getPort());
                try {
                    //启动新客户端
                    tmpClient.start();
                }
                catch (Exception e) {
                    LOGGER.error("Failed to start cluster client[" + newServerConfig.key() + "].", e);
                }
                //启动失败也要加入，否则会导致集群中各个客户的通过一致性hash算出来的位置不一致。导致同一个资源被分配到不同的机器
                newClusterTokenClientList.add(tmpClient);
            }

            //重新构建一致性Hasing工具
            ConsistentHashing<ClusterTokenServiceClient> newConsistentHashing = new ConsistentHashing(VIRTUAL_NODE_SIZE, newClusterTokenClientList);

            //重新计算所有已有资源的位置，这样可以减少对请求的影响
            Map<Long, DynamicClusterTokenClient> newClusterTokenClientMap = new ConcurrentHashMap<>();
            if (ValidatorHelper.isNotEmpty(clusterTokenClientMap)) {
                for (Long flowId : clusterTokenClientMap.keySet()) {
                    newClusterTokenClientMap.put(flowId, newConsistentHashing.getNode(flowId));
                }
            }

            //所有数据都计算完，覆盖原有属性
            clusterTokenClientMap = newClusterTokenClientMap;
            clusterTokenClientList = newClusterTokenClientList;
            consistentHashing = newConsistentHashing;
            LOGGER.info("The new server list is " + newServerConfigMap.keySet());
        }

        if (ValidatorHelper.isNotEmpty(toDeleteServerList)) {
            //异步关闭，等待所有链接都返回
            ExecutorHelper.getScheduledExecutorService().schedule(() -> {
                //最后关闭就客户端
                //关闭掉删除的客户端
                for (ClusterTokenServiceClient client : toDeleteServerList) {
                    try {
                        client.stop();
                        LOGGER.info("Succeeced to close client[" + client.desc() + "].");
                    }
                    catch (Exception e) {
                        LOGGER.error("Failed to stop client[" + client.desc() + "].", e);
                    }
                }
            }, CommonParams.CLOSE_CLIENT_DELAY_SEC, TimeUnit.SECONDS);
        }
    }

    private String hostKey(String ip, Integer port) {
        return ip + ":" + port;
    }

    private void startClientIfScheduled() throws Exception {
        //shouldStart表示是否需要启动，只要系统已经启动了shouldStart=true.(不是表示是否是启动状态)
        //所以客户端变更的时候，因为shouldStart=true就会启动新的客户端了连接了
        if (shouldStart.get()) {
            List<ClusterTokenServiceClient> clientList = clusterTokenClientList;
            if (ValidatorHelper.isNotEmpty(clientList)) {
                for (ClusterTokenClient client : clientList) {
                    client.start();
                }
            }
            else {
                LOGGER.warn("[MappedClusterTokenClient] Cannot start transport client: client not created");
            }
        }
    }

    private void stopClientIfStarted() {
        List<ClusterTokenServiceClient> clientList = clusterTokenClientList;
        if (ValidatorHelper.isNotEmpty(clientList)) {
            for (ClusterTokenClient client : clientList) {
                try {
                    client.stop();
                }
                catch (Exception e) {
                    LOGGER.error("Failed to stop client.", e);
                }
            }
        }
    }

    @Override
    public void start() throws Exception {
        if (shouldStart.compareAndSet(false, true)) {
            startClientIfScheduled();
        }
    }

    @Override
    public void stop() throws Exception {
        if (shouldStart.compareAndSet(true, false)) {
            stopClientIfStarted();
        }
    }

    @Override
    public int getState() {
        List<ClusterTokenServiceClient> tmpClusterTokenClientList = clusterTokenClientList;
        if (ValidatorHelper.isEmpty(tmpClusterTokenClientList)) {
            return ClientConstants.CLIENT_STATUS_OFF;
        }

        for (ClusterTokenServiceClient clusterTokenServiceClient : tmpClusterTokenClientList) {
            if (clusterTokenServiceClient.getState() == ClientConstants.CLIENT_STATUS_STARTED) {
                return ClientConstants.CLIENT_STATUS_STARTED;
            }
        }

        return ClientConstants.CLIENT_STATUS_OFF;
    }

    @Override
    public DynamicClusterTokenClient selectClusterTokenClient(long flowId) {
        return pickClient(flowId);
    }

    private DynamicClusterTokenClient pickClient(Long flowId) {
        final ConsistentHashing<ClusterTokenServiceClient> tmpConsistentHashing = consistentHashing;
        List<ClusterTokenServiceClient> tmpClusterTokenClientList = clusterTokenClientList;
        Map<Long, DynamicClusterTokenClient> tmpClusterTokenClientMap = clusterTokenClientMap;

        if (null == tmpConsistentHashing || ValidatorHelper.isEmpty(tmpClusterTokenClientList) || null == tmpClusterTokenClientMap) {
            return null;
        }

        //对于单个的情况直接快速响应
        if (tmpClusterTokenClientList.size() == 1) {
            return tmpClusterTokenClientList.get(0);
        }

        return tmpClusterTokenClientMap.computeIfAbsent(flowId, (k) -> {
            return tmpConsistentHashing.getNode(flowId);
        });
    }

    public List<ClusterClientStateEntity> listClusterClientState() {
        List<ClusterTokenServiceClient> tmpTokenClientList = this.clusterTokenClientList;
        if (ValidatorHelper.isEmpty(tmpTokenClientList)) {
            return new ArrayList<>();
        }

        List<ClusterClientStateEntity> stateList = new ArrayList<>(tmpTokenClientList.size());
        for (ClusterTokenServiceClient client : tmpTokenClientList) {
            stateList.add(new ClusterClientStateEntity().setServerHost(client.getIp()).setServerPort(client.getPort()).setClientState(client.getState()).setRequestTimeout(ClusterClientConfigManager.getRequestTimeout()));
        }
        return stateList;
    }
}