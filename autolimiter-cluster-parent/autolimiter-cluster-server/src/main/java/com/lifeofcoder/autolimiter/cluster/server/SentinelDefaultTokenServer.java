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
package com.lifeofcoder.autolimiter.cluster.server;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.server.ClusterTokenServer;
import com.alibaba.csp.sentinel.init.InitExecutor;
import com.alibaba.csp.sentinel.util.HostNameUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.lifeofcoder.autolimiter.cluster.common.registry.ConfigSupplierRegistry;
import com.lifeofcoder.autolimiter.cluster.server.config.ClusterServerConfigManager;
import com.lifeofcoder.autolimiter.cluster.server.config.ServerTransportConfig;
import com.lifeofcoder.autolimiter.cluster.server.config.ServerTransportConfigObserver;
import com.lifeofcoder.autolimiter.cluster.server.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class SentinelDefaultTokenServer implements ClusterTokenServer {
    private static Logger LOGGER = LoggerFactory.getLogger(SentinelDefaultTokenServer.class);

    private final boolean embedded;

    private ClusterTokenServer server;
    private final AtomicBoolean shouldStart = new AtomicBoolean(false);

    static {
        InitExecutor.doInit();
    }

    public SentinelDefaultTokenServer() {
        this(false);
    }

    public SentinelDefaultTokenServer(boolean embedded) {
        this.embedded = embedded;
        ClusterServerConfigManager.addTransportConfigChangeObserver(new ServerTransportConfigObserver() {
            @Override
            public void onTransportConfigChange(ServerTransportConfig config) {
                changeServerConfig(config);
            }
        });
        initNewServer();
    }

    private void initNewServer() {
        if (server != null) {
            return;
        }
        int port = ClusterServerConfigManager.getPort();
        if (port > 0) {
            this.server = new NettyTransportServer(port);
        }
    }

    private synchronized void changeServerConfig(ServerTransportConfig config) {
        if (null == config && !config.isValid()) {
            LOGGER.error("The change server config is invalid." + config);
            return;
        }
        try {
            if (server != null) {
                stopServer();
            }
            this.server = new NettyTransportServer(config);
            startServerIfScheduled();
        }
        catch (Exception ex) {
            LOGGER.warn("[SentinelDefaultTokenServer] Failed to apply modification to token server", ex);
        }
    }

    private void startServerIfScheduled() throws Exception {
        if (shouldStart.get()) {
            if (server != null) {
                server.start();
                ClusterStateManager.markToServer();
                if (embedded) {
                    LOGGER.info("[SentinelDefaultTokenServer] Running in embedded mode");
                    handleEmbeddedStart();
                }
            }
        }
    }

    private void stopServer() throws Exception {
        if (server != null) {
            server.stop();
            if (embedded) {
                handleEmbeddedStop();
            }
        }
    }

    private void handleEmbeddedStop() {
        String namespace = ConfigSupplierRegistry.getNamespaceSupplier().get();
        if (StringUtil.isNotEmpty(namespace)) {
            ConnectionManager.removeConnection(namespace, HostNameUtil.getIp());
        }
    }

    private void handleEmbeddedStart() {
        String namespace = ConfigSupplierRegistry.getNamespaceSupplier().get();
        if (StringUtil.isNotEmpty(namespace)) {
            // Mark server global mode as embedded.
            ClusterServerConfigManager.setEmbedded(true);
            if (!ClusterServerConfigManager.getNamespaceSet().contains(namespace)) {
                Set<String> namespaceSet = new HashSet<>(ClusterServerConfigManager.getNamespaceSet());
                namespaceSet.add(namespace);
                ClusterServerConfigManager.loadServerNamespaceSet(namespaceSet);
            }

            // Register self to connection group.
            ConnectionManager.addConnection(namespace, HostNameUtil.getIp());
        }
    }

    @Override
    public void start() throws Exception {
        if (shouldStart.compareAndSet(false, true)) {
            startServerIfScheduled();
        }
    }

    @Override
    public void stop() throws Exception {
        if (shouldStart.compareAndSet(true, false)) {
            stopServer();
        }
    }
}
