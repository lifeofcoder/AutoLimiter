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

import com.lifeofcoder.autolimiter.common.config.ClusterClientConfig;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public interface ServerChangeObserver {

    /**
     * 服务端Topo结构变更
     */
    void onClusterServerTopoChange(ClusterServerTopoConfig assignConfig);

    /**
     * 集群客户端配置变更
     */
    void onClusterClientConfigChange(ClusterClientConfig clusterClientConfig);
}
