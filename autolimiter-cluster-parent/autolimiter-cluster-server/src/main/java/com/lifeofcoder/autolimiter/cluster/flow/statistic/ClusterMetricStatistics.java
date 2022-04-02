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
package com.lifeofcoder.autolimiter.cluster.flow.statistic;

import com.alibaba.csp.sentinel.util.AssertUtil;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.metric.ClusterMetric;
import com.lifeofcoder.autolimiter.cluster.server.config.ClusterServerConfigManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public final class ClusterMetricStatistics {

    private static final Map<Long, ClusterMetric> METRIC_MAP = new ConcurrentHashMap<>();

    private static volatile boolean recordHistory;

    public static void setRecordHistory(boolean recordHistory) {
        ClusterMetricStatistics.recordHistory = recordHistory;
    }

    public static boolean isRecordHistory() {
        return recordHistory;
    }

    public static void clear() {
        METRIC_MAP.clear();
    }

    public static void putMetric(long id, ClusterMetric metric) {
        AssertUtil.notNull(metric, "Cluster metric cannot be null");
        METRIC_MAP.put(id, metric);
    }

    /**
     * 如果不存在就设置，同时返回最新的数据
     */
    public static ClusterMetric computeMetricIfAbsent(long id, final ClusterMetric metric) {
        AssertUtil.notNull(metric, "Cluster metric cannot be null");

        return METRIC_MAP.computeIfAbsent(id, k -> metric);
    }

    public static void removeMetric(long id) {
        METRIC_MAP.remove(id);
    }

    public static ClusterMetric getMetric(long id) {
        return METRIC_MAP.get(id);
    }

    public static ClusterMetric getMetricAndRecordHistory(long id) {
        ClusterMetric clusterMetric = METRIC_MAP.get(id);
        //开启历史日志记录
        if (recordHistory) {
            clusterMetric.tryRecordHistory(id);
        }
        return clusterMetric;
    }

    public static void resetFlowMetrics() {
        for (ClusterMetric value : METRIC_MAP.values()) {
            value.reset(ClusterServerConfigManager.getSampleCount(), ClusterServerConfigManager.getIntervalMs());
        }
    }

    private ClusterMetricStatistics() {
    }
}
