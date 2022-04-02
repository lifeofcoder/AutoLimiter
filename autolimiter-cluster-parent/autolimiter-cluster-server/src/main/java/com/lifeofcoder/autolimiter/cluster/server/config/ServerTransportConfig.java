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
package com.lifeofcoder.autolimiter.cluster.server.config;

import com.lifeofcoder.autolimiter.cluster.common.ClusterConstants;
import io.netty.util.internal.SystemPropertyUtil;

import java.util.Objects;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ServerTransportConfig {

    private static final int DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2));
    public static final int DEFAULT_IDLE_SECONDS = 600;

    private int port = ClusterConstants.DEFAULT_CLUSTER_SERVER_PORT;
    private int idleSeconds = DEFAULT_IDLE_SECONDS;
    private int bossThreads = 1;
    private int workThreads = DEFAULT_EVENT_LOOP_THREADS;
    private int soBacklog = 128;
    private int soSndbuf = 32 * 1024;
    private int connectTimeoutMillis = 10000;
    private boolean tcpNodelay = true;
    private int soRcvbuf = 32 * 1024;
    private boolean epoll = true;

    public boolean isValid() {
        return port > 0 && bossThreads > 0 && workThreads > 0 && idleSeconds > 0 && soBacklog > 0 && soSndbuf > 0 && soRcvbuf > 0 && connectTimeoutMillis > 0 && soRcvbuf > 0;
    }

    public int getPort() {
        return port;
    }

    public ServerTransportConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public int getIdleSeconds() {
        return idleSeconds;
    }

    public ServerTransportConfig setIdleSeconds(int idleSeconds) {
        this.idleSeconds = idleSeconds;
        return this;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkThreads() {
        return workThreads;
    }

    public void setWorkThreads(int workThreads) {
        this.workThreads = workThreads;
    }

    public int getSoBacklog() {
        return soBacklog;
    }

    public void setSoBacklog(int soBacklog) {
        this.soBacklog = soBacklog;
    }

    public int getSoSndbuf() {
        return soSndbuf;
    }

    public void setSoSndbuf(int soSndbuf) {
        this.soSndbuf = soSndbuf;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public boolean isTcpNodelay() {
        return tcpNodelay;
    }

    public void setTcpNodelay(boolean tcpNodelay) {
        this.tcpNodelay = tcpNodelay;
    }

    public int getSoRcvbuf() {
        return soRcvbuf;
    }

    public void setSoRcvbuf(int soRcvbuf) {
        this.soRcvbuf = soRcvbuf;
    }

    public boolean isEpoll() {
        return epoll;
    }

    public void setEpoll(boolean epoll) {
        this.epoll = epoll;
    }

    @Override
    public String toString() {
        return "ServerTransportConfig{" + "port=" + port + ", idleSeconds=" + idleSeconds + ", bossThreads=" + bossThreads + ", workThreads=" + workThreads + ", soBacklog=" + soBacklog + ", soSndbuf=" + soSndbuf + ", connectTimeoutMillis="
               + connectTimeoutMillis + ", tcpNodelay=" + tcpNodelay + ", soRcvbuf=" + soRcvbuf + ", epoll=" + epoll + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ServerTransportConfig that = (ServerTransportConfig) o;
        return port == that.port && idleSeconds == that.idleSeconds && bossThreads == that.bossThreads && workThreads == that.workThreads && soBacklog == that.soBacklog && soSndbuf == that.soSndbuf && connectTimeoutMillis == that.connectTimeoutMillis
               && tcpNodelay == that.tcpNodelay && soRcvbuf == that.soRcvbuf && epoll == that.epoll;
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, idleSeconds, bossThreads, workThreads, soBacklog, soSndbuf, connectTimeoutMillis, tcpNodelay, soRcvbuf, epoll);
    }
}
