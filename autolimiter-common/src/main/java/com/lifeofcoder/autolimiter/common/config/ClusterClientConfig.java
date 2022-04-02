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
package com.lifeofcoder.autolimiter.common.config;

import com.lifeofcoder.autolimiter.common.model.RouteMode;

import java.util.Objects;

/**
 * 集群客户端配置信息
 */
public class ClusterClientConfig {
    public static final String KEY = "CLUSTER_CONFIG";

    private static final int DEFAULT_REQUEST_TIMEOUT = 20;
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 10 * 1000;
    private static final double DEF_MIN_AVAILABLE_RATIO = 0.95D;

    /**
     * 请求超时
     */
    private Integer requestTimeout = DEFAULT_REQUEST_TIMEOUT;

    /**
     * 配置连接超时
     */
    private Integer connectTimeout = DEFAULT_CONNECT_TIMEOUT_MILLIS;

    /**
     * 路由模式：rule：表示根据不同的rule计算不同的节点，system(默认):同一个系统使用同一个节点
     */
    private RouteMode routeMode = RouteMode.SYSTEM;

    /**
     * 可用率，比如0.9就表示节点可用率低于90%则表示不可用需要降级
     */
    private Double availableRatio = DEF_MIN_AVAILABLE_RATIO;

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public ClusterClientConfig setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public RouteMode getRouteMode() {
        return routeMode;
    }

    public void setRouteMode(String routeMode) {
        this.routeMode = RouteMode.of(routeMode);
    }

    public Double getAvailableRatio() {
        return availableRatio;
    }

    public void setAvailableRatio(Double availableRatio) {
        this.availableRatio = availableRatio;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClusterClientConfig that = (ClusterClientConfig) o;
        return Objects.equals(requestTimeout, that.requestTimeout) && Objects.equals(connectTimeout, that.connectTimeout) && Objects.equals(routeMode, that.routeMode) && Objects.equals(availableRatio, that.availableRatio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestTimeout, connectTimeout, routeMode, availableRatio);
    }

    @Override
    public String toString() {
        return "ClusterClientConfig{" + "requestTimeout=" + requestTimeout + ", connectTimeout=" + connectTimeout + ", routeMode='" + routeMode + '\'' + ", availableRatio=" + availableRatio + '}';
    }
}
