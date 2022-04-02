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
package com.lifeofcoder.autolimiter.cluster.flow.statistic.metric;

import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.TimeUtil;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.lifeofcoder.autolimiter.cluster.flow.client.ClusterRuleClients;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricNode;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.data.ClusterFlowEvent;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.data.ClusterMetricBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public class ClusterMetric {
    private static Logger LOGGER = LoggerFactory.getLogger(ClusterMetric.class);

    private ClusterMetricLeapArray metric;

    /**
     * 历史衡量数据，只记录15秒
     */
    private ConcurrentLinkedHashMap<Long, ClusterMetricNode> historyMetricMap;

    /**
     * 上一次记录的秒
     */
    private volatile AtomicLong lastRecordSecondAtomic;

    /**
     * 当前的最大流控值
     */
    private int currentMaxCount;

    /**
     * 还剩余多少流控值可用
     */
    private AtomicInteger remainAtomic;

    private ClusterRuleClients clusterRuleClients;

    public ClusterMetric(long flowId, int sampleCount, int intervalInMs, int maxCount) {
        AssertUtil.isTrue(sampleCount > 0, "sampleCount should be positive");
        AssertUtil.isTrue(intervalInMs > 0, "interval should be positive");
        AssertUtil.isTrue(intervalInMs % sampleCount == 0, "time span needs to be evenly divided");
        remainAtomic = new AtomicInteger(maxCount);
        reset(sampleCount, intervalInMs);
        clusterRuleClients = new ClusterRuleClients(flowId, this);
        currentMaxCount = maxCount;
    }

    public synchronized void reset(int sampleCount, int intervalInMs) {
        metric = new ClusterMetricLeapArray(sampleCount, intervalInMs);
        historyMetricMap = new ConcurrentLinkedHashMap.Builder<Long, ClusterMetricNode>().concurrencyLevel(32).initialCapacity(16).maximumWeightedCapacity(32).build();
        lastRecordSecondAtomic = new AtomicLong(TimeUtil.currentTimeMillis() / 1000);
    }

    /**
     * 尝试更新MaxCount,并跳转remain
     */
    public synchronized void tryUpdateMaxCount(int newMaxCount) {
        if (currentMaxCount == newMaxCount) {
            return;
        }

        //可正可负
        int remain = newMaxCount - currentMaxCount;
        addRemain(remain);
        currentMaxCount = newMaxCount;
    }

    public ClusterMetricNode getHistory(long timeSecond) {
        return historyMetricMap.get(timeSecond);
    }

    public void tryRecordHistory(long flowId) {
        long currentMillis = TimeUtil.currentTimeMillis();
        //提前5毫秒，如果否则刚刚跳一格，那么1秒10格，就只能统计9格的数据。会导致统计少十分之一
        long currentSecond = (currentMillis + 5) / 1000;
        long lastSecond = lastRecordSecondAtomic.get();
        if (currentSecond <= lastSecond) {
            return;
        }

        //被被别人抢锁了
        if (!lastRecordSecondAtomic.compareAndSet(lastSecond, currentSecond)) {
            return;
        }

        double pass = getAvg(ClusterFlowEvent.PASS);
        double block = getAvg(ClusterFlowEvent.BLOCK);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(flowId + ":" + currentSecond + ":" + pass + ":" + block + ":" + metric.getIntervalInSecond());
        }

        //TODO 考虑异步线程处理
        ClusterMetricNode clusterMetricNode = new ClusterMetricNode().setBlockQps(block).setTimestamp(currentSecond * 1000);

        clusterMetricNode.setPassQps(pass);
        historyMetricMap.putIfAbsent(currentSecond, clusterMetricNode);
    }

    public void add(ClusterFlowEvent event, long count) {
        metric.currentWindow().value().add(event, count);
    }

    public long getCurrentCount(ClusterFlowEvent event) {
        return metric.currentWindow().value().get(event);
    }

    /**
     * Get total sum for provided event in {@code intervalInSec}.
     *
     * @param event event to calculate
     * @return total sum for event
     */
    public long getSum(ClusterFlowEvent event) {
        long sum = 0;

        List<ClusterMetricBucket> buckets = metric.values();
        for (ClusterMetricBucket bucket : buckets) {
            sum += bucket.get(event);
        }
        return sum;
    }

    /**
     * Get average count for provided event per second.
     *
     * @param event event to calculate
     * @return average count per second for event
     */
    public double getAvg(ClusterFlowEvent event) {
        return getSum(event) / metric.getIntervalInSecond();
    }

    /**
     * Try to pre-occupy upcoming buckets.
     *
     * @return time to wait for next bucket (in ms); 0 if cannot occupy next buckets
     */
    public int tryOccupyNext(ClusterFlowEvent event, int acquireCount, double threshold) {
        double latestQps = getAvg(ClusterFlowEvent.PASS);
        if (!canOccupy(event, acquireCount, latestQps, threshold)) {
            return 0;
        }
        metric.addOccupyPass(acquireCount);
        add(ClusterFlowEvent.WAITING, acquireCount);
        return 1000 / metric.getSampleCount();
    }

    private boolean canOccupy(ClusterFlowEvent event, int acquireCount, double latestQps, double threshold) {
        long headPass = metric.getFirstCountOfWindow(event);
        long occupiedCount = metric.getOccupiedCount(event);
        //  bucket to occupy (= incoming bucket)
        //       ↓
        // | head bucket |    |    |    | current bucket |
        // +-------------+----+----+----+----------- ----+
        //   (headPass)
        return latestQps + (acquireCount + occupiedCount) - headPass <= threshold;
    }

    public int getRemain() {
        return remainAtomic.get();
    }

    public void addRemain(int remain) {
        remainAtomic.addAndGet(remain);
    }

    /**
     * 尝试更新剩余的值
     */
    public boolean tryUpdateRemain(int oldValue, int newValue) {
        return remainAtomic.compareAndSet(oldValue, newValue);
    }

    public ClusterRuleClients getClusterRuleClients() {
        return clusterRuleClients;
    }
}
