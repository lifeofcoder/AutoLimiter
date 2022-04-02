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

import com.alibaba.csp.sentinel.cluster.TokenServerDescriptor;
import com.alibaba.csp.sentinel.cluster.client.ClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.cluster.client.consts.CommonParams;
import com.lifeofcoder.autolimiter.cluster.client.dynamic.DynamicClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.command.entity.ClusterClientStateEntity;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;
import com.lifeofcoder.autolimiter.common.utils.ExecutorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link ClusterTokenClient}.
 * 系统路由
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
public class SystemClusterTokenClient implements ChangeableClusterTokenClient {
    private static Logger LOGGER = LoggerFactory.getLogger(SystemClusterTokenClient.class);

    private volatile ClusterTokenServiceClient tokenClient;

    private TokenServerDescriptor serverDescriptor;

    //这个字段表示的意思是，系统是否需要启动。而不是能不能启动
    private final AtomicBoolean shouldStart = new AtomicBoolean(false);

    @Override
    public synchronized void clusterServerTopoChanged(ClusterServerTopoConfig topoConfig) {
        try {
            ClusterTokenServiceClient newClient = null;
            TokenServerDescriptor newDescripter = null;
            //构建新客户端并启动
            if (topoConfig != null) {
                newClient = new ClusterTokenServiceClient(topoConfig.getServerHost(), topoConfig.getServerPort());
                newClient.start();
                newDescripter = new TokenServerDescriptor(topoConfig.getServerHost(), topoConfig.getServerPort());
            }

            //使用新客户端
            ClusterTokenServiceClient oldClient = tokenClient;
            TokenServerDescriptor oldTokenServerDescriptor = serverDescriptor;
            this.tokenClient = newClient;
            this.serverDescriptor = newDescripter;

            if (oldClient != null) {
                //异步关闭，等待所有链接都返回
                ExecutorHelper.getScheduledExecutorService().schedule(() -> {
                    //最后关闭就客户端
                    try {
                        oldClient.stop();
                        LOGGER.info("Succeeced to close client[" + toDesc(oldTokenServerDescriptor) + "].");
                    }
                    catch (Exception e) {
                        LOGGER.error("Failed to stop cluster token client[" + toDesc(oldTokenServerDescriptor) + "].", e);
                    }
                }, CommonParams.CLOSE_CLIENT_DELAY_SEC, TimeUnit.SECONDS);
            }
            LOGGER.info("[DefaultClusterTokenClient] New client created: " + serverDescriptor);
        }
        catch (Exception ex) {
            LOGGER.warn("[DefaultClusterTokenClient] Failed to change remote token server", ex);
        }
    }

    private String toDesc(TokenServerDescriptor tokenServerDescriptor) {
        if (null == tokenServerDescriptor) {
            return "Null";
        }
        else {
            return tokenServerDescriptor.toString();
        }
    }

    private void startClientIfScheduled() throws Exception {
        //shouldStart表示是否需要启动，只要系统已经启动了shouldStart=true.(不是表示是否是启动状态)
        //所以客户端变更的时候，因为shouldStart=true就会启动新的客户端了连接了
        if (shouldStart.get()) {
            if (tokenClient != null) {
                tokenClient.start();
            }
            else {
                LOGGER.warn("[DefaultClusterTokenClient] Cannot start transport client: client not created");
            }
        }
    }

    private void stopClientIfStarted() throws Exception {
        if (shouldStart.compareAndSet(true, false)) {
            if (tokenClient != null) {
                tokenClient.stop();
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
        stopClientIfStarted();
    }

    @Override
    public int getState() {
        if (tokenClient == null) {
            return ClientConstants.CLIENT_STATUS_OFF;
        }

        return tokenClient.getState();
    }

    @Override
    public DynamicClusterTokenClient selectClusterTokenClient(long flowId) {
        DynamicClusterTokenClient tmpTokenClient = tokenClient;
        return tmpTokenClient;
    }

    @Override
    public List<ClusterClientStateEntity> listClusterClientState() {
        ClusterClientStateEntity state = new ClusterClientStateEntity().setClientState(getState()).setRequestTimeout(ClusterClientConfigManager.getRequestTimeout());
        if (null != serverDescriptor) {
            state.setServerHost(serverDescriptor.getHost());
            state.setServerPort(serverDescriptor.getPort());
        }
        return Arrays.asList(state);
    }
}
