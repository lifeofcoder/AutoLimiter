package com.lifeofcoder.autolimiter.cluster.client.dynamic;

import com.lifeofcoder.autolimiter.cluster.client.consts.ClusterCheckResult;
import com.lifeofcoder.autolimiter.common.counter.SlidingWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群流控计数器
 *
 * @author xbc
 * @date 2022/3/17
 */
public class DynamicFlowCounter {
    private static Logger LOGGER = LoggerFactory.getLogger(DynamicFlowCounter.class);

    private static final int TEN_SECOND = 10_000;

    /**
     * 总请求数据
     */
    private SlidingWindow totalCountQps;

    /**
     * 通过的请求数据
     */
    private SlidingWindow passCountQps;

    /**
     * 最大上限，即流控最大值,由集群节点动态控制
     */
    private volatile int maxCount;

    /**
     * 资源ID
     */
    private long flowId;

    /**
     * 加载完成
     */
    private volatile boolean loaded;

    /**
     * 启动时间
     */
    private volatile long startTimeMs = -1;

    private boolean deleted;

    /**
     * 集群可用,可以接受一定的延迟，所以可以不定义volatile
     */
    private volatile boolean clusterAvailable = true;

    public DynamicFlowCounter(long flowId, int maxCount) {
        this.flowId = flowId;
        updateMaxCount(maxCount);

        //将一秒钟分配为10个窗口
        totalCountQps = new SlidingWindow(10);
        passCountQps = new SlidingWindow(10);
    }

    /**
     * 是否可以通过
     */
    public ClusterCheckResult canPass() {
        ClusterCheckResult result = doCanPass();
        if (result == ClusterCheckResult.PASS || result == ClusterCheckResult.FALLBACK) {
            passCountQps.add();
        }
        return result;
    }

    /**
     * 是否可以通过
     */
    private ClusterCheckResult doCanPass() {
        totalCountQps.add();

        if (!loaded) {
            //第一次访问
            if (startTimeMs <= 0) {
                startTimeMs = System.currentTimeMillis();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The request for[" + flowId + "] is fallback because it's not initiated.");
                }
                return ClusterCheckResult.FALLBACK;
            }
            //预热完成
            else if (System.currentTimeMillis() - startTimeMs > TEN_SECOND) {
                loaded = true;
            }
            //10秒之内都是启动预热阶段，此时都是降级本地
            else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The request for[" + flowId + "] is fallback because it's hot loading.");
                }
                return ClusterCheckResult.FALLBACK;
            }
        }

        //集群不可用暂时降级到本地
        if (!clusterAvailable) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The request for [" + flowId + "] is fallback because the cluster is crashed.");
            }
            return ClusterCheckResult.FALLBACK;
        }

        long currentPassQps = passCountQps.count();
        long currentMaxCount = maxCount;
        ClusterCheckResult result = currentPassQps <= currentMaxCount ? ClusterCheckResult.PASS : ClusterCheckResult.BLOCK;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The result of cluster flow for [" + flowId + "] is [" + result + "] with information [currentQPS:" + currentPassQps + ", maxCount:" + currentMaxCount + "].");
        }
        return result;
    }

    /**
     * 更新总数
     */
    public synchronized void updateMaxCount(int maxCount) {
        if (maxCount < 0) {
            maxCount = 0;
        }

        //减少不必要的写入
        if (clusterAvailable == false) {
            clusterAvailable = true;
        }

        if (this.maxCount != maxCount) {
            this.maxCount = maxCount;
        }
    }

    /**
     * 获取资源ID
     */
    public long getFlowId() {
        return flowId;
    }

    public void delete() {
        deleted = true;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getCurrentTotalCount() {
        //计数器不可能超过int,所以这里可以强制转换，减少后面发送到counter server的贷款
        return (int) totalCountQps.count();
    }

    /**
     * 集群不可用暂时降级到本地
     */
    public void clusterCrashed() {
        clusterAvailable = false;
    }
}