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

import com.lifeofcoder.autolimiter.common.utils.ValidatorHelper;

import java.util.List;
import java.util.Objects;

/**
 * 集群服务端Topo配置（可用节点信息）
 */
public class ClusterServerTopoConfig {
    public static final String KEY = "CLUSTER_COUNTER_SERVER";

    private String serverHost;

    private Integer serverPort;

    private List<ClusterServerConfigItem> servers;

    public ClusterServerTopoConfig() {
    }

    public List<ClusterServerConfigItem> getServers() {
        return servers;
    }

    public ClusterServerTopoConfig setServers(List<ClusterServerConfigItem> servers) {
        this.servers = servers;
        return this;
    }

    public String getServerHost() {
        return serverHost;
    }

    public ClusterServerTopoConfig setServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public ClusterServerTopoConfig setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClusterServerTopoConfig that = (ClusterServerTopoConfig) o;
        return Objects.equals(serverHost, that.serverHost) && Objects.equals(serverPort, that.serverPort) && Objects.equals(servers, that.servers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverHost, serverPort, servers);
    }

    @Override
    public String toString() {
        return "ClusterServerTopoConfig{, + serverHost='" + serverHost + '\'' + ", serverPort='" + serverPort + '\'' + ", servers=" + servers + '}';
    }

    public boolean isValid() {
        return ValidatorHelper.isNotEmpty(serverHost) && serverPort != null && serverPort > 0 && null != servers && !servers.isEmpty();
    }
}
