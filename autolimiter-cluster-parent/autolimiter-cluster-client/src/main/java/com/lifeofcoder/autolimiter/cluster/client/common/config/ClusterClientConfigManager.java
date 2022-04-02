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
package com.lifeofcoder.autolimiter.cluster.client.common.config;

import com.alibaba.csp.sentinel.util.AssertUtil;
import com.lifeofcoder.autolimiter.cluster.client.consts.CommonParams;
import com.lifeofcoder.autolimiter.cluster.common.ClusterConstants;
import com.lifeofcoder.autolimiter.common.config.ClusterClientConfig;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;
import com.lifeofcoder.autolimiter.common.model.RouteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public final class ClusterClientConfigManager {
    private static Logger LOGGER = LoggerFactory.getLogger(ClusterClientConfigManager.class);

    private static volatile ClusterServerTopoConfig clusterServerTopoConfig;

    private static volatile ClusterClientConfig clusterClientConfig;

    private static final List<ServerChangeObserver> SERVER_CHANGE_OBSERVERS = new ArrayList<>();

    private ClusterClientConfigManager() {
    }

    /**
     * 注册服务变更监听器
     */
    public static void addServerChangeObserver(ServerChangeObserver observer) {
        AssertUtil.notNull(observer, "observer cannot be null");
        SERVER_CHANGE_OBSERVERS.add(observer);
    }

    /**
     * 集群客户端配置变更
     */
    public static synchronized void clusterClientConfigChanged(ClusterClientConfig newClusterClientConfig) {
        if (null == newClusterClientConfig) {
            LOGGER.error("There is no need to update cluster client config because the new ClusterClientConfig is null.");
            return;
        }

        //没有改变，不处理
        if (Objects.equals(clusterClientConfig, newClusterClientConfig)) {
            LOGGER.info("There is no need to update cluster client config because the new ClusterClientConfig is not changed.");
            return;
        }

        clusterClientConfig = newClusterClientConfig;
        LOGGER.info("Try to notify " + SERVER_CHANGE_OBSERVERS.size() + " ServerChangeObserver[ClusterClientConfigChanged]");
        for (ServerChangeObserver observer : SERVER_CHANGE_OBSERVERS) {
            LOGGER.info("Try to notify " + observer.getClass().getSimpleName() + " ServerChangeObserver[ClusterClientConfigChanged]");
            observer.onClusterClientConfigChange(newClusterClientConfig);
        }

        LOGGER.info("The ClusterClientConfig has been updated to " + newClusterClientConfig);
    }

    /**
     * 集群服务器节点变更通知
     */
    public static synchronized void clusterServerTopoChanged(ClusterServerTopoConfig topoConfig) {
        if (Objects.equals(clusterServerTopoConfig, topoConfig)) {
            LOGGER.info("The ClusterServerTopoConfig doesn't changed.");
            return;
        }

        clusterServerTopoConfig = topoConfig;
        LOGGER.info("Try to notify " + SERVER_CHANGE_OBSERVERS.size() + " ServerChangeObserver[ClusterServerTopoChanged]");
        for (ServerChangeObserver observer : SERVER_CHANGE_OBSERVERS) {
            LOGGER.info("Try to notify " + observer.getClass().getSimpleName() + " ServerChangeObserver[ClusterServerTopoChanged]");
            observer.onClusterServerTopoChange(topoConfig);
        }

        LOGGER.info("The ClusterServerTopoConfig has been updated to " + topoConfig);
    }

    public static ClusterServerTopoConfig currentClusterServerTopoConfig() {
        return clusterServerTopoConfig;
    }

    public static ClusterClientConfig currentClusterClientConfig() {
        return clusterClientConfig;
    }

    /**
     * 获取路由模式
     */
    public static RouteMode currentRouteMode() {
        ClusterClientConfig tmpClusterClientConfig = clusterClientConfig;
        if (null == tmpClusterClientConfig) {
            return RouteMode.SYSTEM;
        }

        return tmpClusterClientConfig.getRouteMode();
    }

    private static double doGetAvailableRatio(Double newAvailableRatio) {
        if (isValidAvailableRatio(newAvailableRatio)) {
            return newAvailableRatio;
        }
        return CommonParams.DEF_MIN_AVAILABLE_RATIO;
    }

    private static boolean isValidAvailableRatio(Double newAvailableRatio) {
        if (null != newAvailableRatio && newAvailableRatio > 0 && newAvailableRatio < 1) {
            return true;
        }
        return false;
    }

    public static double getMinAvailableRatio(Double customizedRatio) {
        if (isValidAvailableRatio(customizedRatio)) {
            return customizedRatio.doubleValue();
        }

        return getGlobalMinAvaiableRatio();
    }

    public static double getGlobalMinAvaiableRatio() {
        ClusterClientConfig clusterClientConfig = currentClusterClientConfig();
        if (null == clusterClientConfig) {
            return CommonParams.DEF_MIN_AVAILABLE_RATIO;
        }

        return doGetAvailableRatio(clusterClientConfig.getAvailableRatio());
    }

    /**
     * 全局提供超时时间
     */
    public static int getRequestTimeout() {
        ClusterClientConfig tmpClusterClientConfig = clusterClientConfig;
        if (null == tmpClusterClientConfig || tmpClusterClientConfig.getRequestTimeout() == null) {
            return ClusterConstants.DEFAULT_REQUEST_TIMEOUT;
        }
        else {
            return tmpClusterClientConfig.getRequestTimeout();
        }
    }

    /**
     * 全局提供连接超时时间
     */
    public static int getConnectTimeout() {
        ClusterClientConfig tmpClusterClientConfig = clusterClientConfig;
        if (null == tmpClusterClientConfig || tmpClusterClientConfig.getConnectTimeout() == null) {
            return ClusterConstants.DEFAULT_CONNECT_TIMEOUT_MILLIS;
        }
        else {
            return tmpClusterClientConfig.getConnectTimeout();
        }
    }
}