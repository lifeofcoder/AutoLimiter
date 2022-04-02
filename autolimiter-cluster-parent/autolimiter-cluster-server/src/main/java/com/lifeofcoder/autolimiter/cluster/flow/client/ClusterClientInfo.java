package com.lifeofcoder.autolimiter.cluster.flow.client;

import com.lifeofcoder.autolimiter.cluster.flow.statistic.metric.ClusterMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 集群客户端节点（双向队列节点）
 *
 * @author xbc
 * @date 2022/3/22
 */
public class ClusterClientInfo {
    private static Logger LOGGER = LoggerFactory.getLogger(ClusterClientInfo.class);

    /**
     * 机器IP
     */
    private Long ip;

    /**
     * 上次访问时间
     */
    private volatile long lastTouchTimeMs;

    /**
     * 当前分配的流控值
     */
    private int maxCount = -1;

    /**
     * 节点统计信息(资源维度)
     */
    private final ClusterMetric clusterMetric;

    /**
     *
     */
    private ClusterRuleClients clusterRuleClients;

    /**
     * 头部或者尾部
     */
    private ClusterClientInfo() {
        clusterMetric = null;
    }

    public ClusterClientInfo(Long ip, ClusterMetric clusterMetric, ClusterRuleClients clusterRuleClients) {
        this.ip = ip;
        this.clusterMetric = clusterMetric;
        this.clusterRuleClients = clusterRuleClients;
        touch();
    }

    public Long getIp() {
        return ip;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void touch() {
        lastTouchTimeMs = System.currentTimeMillis();
    }

    /**
     * 节点是否死亡
     */
    public boolean isDead() {
        if (lastTouchTimeMs <= 0) {
            return false;
        }

        return System.currentTimeMillis() - lastTouchTimeMs > TimeUnit.SECONDS.toMillis(10);
    }

    /**
     * 获取最新的流控值(并发接口)
     * @param calNewMaxCount 新计算出来的流控值
     * @param oldClientMaxCount 客户端当前的流控值
     * @return 计算之后得到的流控值
     */
    public synchronized int achieveCount(int calNewMaxCount, int oldClientMaxCount) {
        //原来的节点已经被释放掉了，之前客户端占用的流控值已经规划给集群，此时oldClientMaxCount无效
        if (maxCount <= 0) {
            maxCount = getCountFromClusterPool(calNewMaxCount);
            return maxCount;
        }

        if (oldClientMaxCount != maxCount) {
            LOGGER.error("The MaxCount between cluster and client is different.[cluster:" + maxCount + ",client:" + oldClientMaxCount + "]");
        }

        //如果按照calNewMaxCount，则还剩余多少需要规划给集群池
        int left = maxCount - calNewMaxCount;

        //没有变化，则不处理
        if (left == 0) {
            return maxCount;
        }
        //需要归还left对应的流控值给集群池
        else if (left > 0) {
            clusterMetric.addRemain(left);
            maxCount = calNewMaxCount;
        }
        //需要从限流池中获取
        else {
            maxCount = getCountFromClusterPool(-left) + maxCount;
        }

        return maxCount;
    }

    /**
     * 从集群流控池中再获取countToGet的流控值
     * 获取失败，则返回默认0,即表示无法从集群池中获取流控值
     */
    private int getCountFromClusterPool(int countToGet) {
        int remain, need = 0, retryTimes = 0;
        boolean updated = false;
        do {
            remain = clusterMetric.getRemain();

            //没有可用限流值，直接返回原来的值
            //可能小于0，如果出现流控值跳转，当跳小了之后，就出现remain为非负数了
            if (remain <= 0) {
                clusterRuleClients.tryDiscard();
                break;
            }

            need = countToGet;
            //剩余不够分配
            if (remain < need) {
                need = remain;
            }
            //如果重试10次之后还是失败，则不新增，直接保持原来的限流值
        }
        while (!(updated = clusterMetric.tryUpdateRemain(remain, remain - need)) && retryTimes++ < 10);
        return updated ? need : 0;
    }

    public void returnCount() {
        if (maxCount > 0) {
            clusterMetric.addRemain(maxCount);
        }
    }
}
