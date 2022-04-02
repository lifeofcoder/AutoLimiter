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
package com.lifeofcoder.autolimiter.cluster.command.entity;

import com.lifeofcoder.autolimiter.common.config.ClusterClientConfig;
import com.lifeofcoder.autolimiter.common.config.ClusterServerConfigItem;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;

import java.io.Serializable;
import java.util.List;

/**
 * @author Eric Zhao
 * @since 1.4.1
 */
public class ClusterClientStateEntity implements Serializable {

    private String serverHost;
    private Integer serverPort;

    /**
     * 可用率，比如0.9就表示节点可用率低于90%则表示不可用需要降级
     */
    private Double availableRatio;

    private Integer clientState;

    private Integer requestTimeout;

    private int clientNum = 1;

    private String routeMode;

    private List<ClusterServerConfigItem> servers;

    public String getServerHost() {
        return serverHost;
    }

    public ClusterClientStateEntity setServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public ClusterClientStateEntity setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public ClusterClientStateEntity setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public Integer getClientState() {
        return clientState;
    }

    public int getClientNum() {
        return clientNum;
    }

    public void setClientNum(int clientNum) {
        this.clientNum = clientNum;
    }

    public String getRouteMode() {
        return routeMode;
    }

    public void setRouteMode(String routeMode) {
        this.routeMode = routeMode;
    }

    public List<ClusterServerConfigItem> getServers() {
        return servers;
    }

    public void setServers(List<ClusterServerConfigItem> servers) {
        this.servers = servers;
    }

    public Double getAvailableRatio() {
        return availableRatio;
    }

    public void setAvailableRatio(Double availableRatio) {
        this.availableRatio = availableRatio;
    }

    public ClusterClientStateEntity setClientState(Integer clientState) {
        this.clientState = clientState;
        return this;
    }

    public ClusterClientConfig toClientConfig() {
        return new ClusterClientConfig().setRequestTimeout(requestTimeout);
    }

    public ClusterServerTopoConfig toAssignConfig() {
        return new ClusterServerTopoConfig().setServerHost(serverHost).setServerPort(serverPort).setServers(servers);
    }

    @Override
    public String toString() {
        return "ClusterClientStateEntity{" + "serverHost='" + serverHost + '\'' + ", serverPort=" + serverPort + ", availableRatio=" + availableRatio + ", clientState=" + clientState + ", requestTimeout=" + requestTimeout + ", clientNum=" + clientNum
               + ", routeMode='" + routeMode + '\'' + ", servers=" + servers + '}';
    }
}