package com.lifeofcoder.autolimiter.cluster.client.toleranter;

import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.cluster.client.consts.CommonParams;
import com.lifeofcoder.autolimiter.common.counter.SlidingWindow;
import com.lifeofcoder.autolimiter.common.utils.ValidatorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 诊断容错器，负责对Counter集群进行诊断与容错
 *
 * @author xbc
 * @date 2021/7/2
 */
public class DiagnoseFaultToleranter {
    private final static Logger LOGGER = LoggerFactory.getLogger(DiagnoseFaultToleranter.class);

    /**
     * 自定义告警Key
     */
    private static final String ALARM_KEY_CLUSTER_CLIENT = "AL_CLUSTER_CLIENT_ALARM";

    /**
     * 最大降级时长
     */
    private static final long MAX_DEGRADE_TIME_MILLIS = 2_000;

    /**
     * 标记服务端是否临时降级，网络原因、服务器重启等
     */
    private volatile boolean serverTempDegraded;

    /**
     * 标记服务端是否宕机，无可用服务端
     */
    private volatile boolean serverCrashed;

    /**
     * 降级开始时间
     */
    private volatile long degradeStartTimeMs;

    /**
     * 最大重试告警次数,即告警时长为2s * 20 = 40秒。因为计数器变更之后，控制台为了应对计数器大量重启的情况，会延迟30秒才会推送最新的数据下来。
     * 所以，这里我们要大于30秒不可用才告警
     */
    private static final long MAX_DEGRADE_ALARM_TIMES = 20;

    /**
     * 降级重试次数
     */
    private AtomicInteger degradeRetryTimes = new AtomicInteger();

    /**
     * 计算失败次数的滑动窗口
     */
    private SlidingWindow failedSlidingWindow = new SlidingWindow(10);

    /**
     * 总请求次数
     */
    private SlidingWindow totalSlidingWindow = new SlidingWindow(10);

    private String ip;

    private Integer port;

    /**
     * 最大失败率
     */
    private double maxFailedRatio;

    private Double serverMinAvailableRatio;

    /**
     * 构造函数
     * @param minAvailableRatio 节点最小可用率(当前需要使用的)
     * @param serverMinAvailableRatio 当前节点自己配置的
     */
    public DiagnoseFaultToleranter(String ip, Integer port, Double serverMinAvailableRatio, double minAvailableRatio) {
        this.ip = ip;
        this.port = port;
        if (null == serverMinAvailableRatio || serverMinAvailableRatio <= 0 || serverMinAvailableRatio >= 1) {
            serverMinAvailableRatio = CommonParams.DEF_MIN_AVAILABLE_RATIO;
        }
        this.serverMinAvailableRatio = serverMinAvailableRatio;
        updateAvailableRatio(minAvailableRatio);
        LOGGER.info("The information of new node are ip = " + ip + ", port = " + port + ", maxFailedRatio = " + maxFailedRatio + ", timeout = " + ClusterClientConfigManager.getRequestTimeout() + ".");
    }

    /**
     * 可以访问
     */
    public boolean canAccess() {
        /**
         * 服务端宕机了
         */
        if (serverCrashed) {
            LOGGER.trace("The counter cluster has crashed.");
            return false;
        }

        long nowMs = System.currentTimeMillis();
        //处于临时降级阶段
        if (serverTempDegraded) {
            //没有超过临时降级超时时间(每两秒钟才能够尝试连接)
            if (nowMs - degradeStartTimeMs <= MAX_DEGRADE_TIME_MILLIS) {
                LOGGER.trace("The cluster client has degraded tempporarily.");
                return false;
            }
            //超过时间，尝试重试,此时返回true，尝试连接counter cluster.
            else {
                int retryTimes = degradeRetryTimes.addAndGet(1);
                //达到告警次数，则告警
                if (retryTimes % MAX_DEGRADE_ALARM_TIMES == 0) {
                    LOGGER.error("Cluster client has been degraded for a long time because counter cluster is not available[counter info->" + ip + ":" + port + "].");
                    //                    AlarmHelper.alarm(ALARM_KEY_CLUSTER_CLIENT, "自动限流降级客户端长时间无法连接全局限流集群[" + ip + ":" + port + "].");
                }

                //尝试重新连接，此时要修改降级开始时间。因为我们需要每两秒钟连接一次
                //连续连接MAX_DEGRADE_ALARM_TIMES次之后，都失败则告警
                degradeStartTimeMs = nowMs;

                LOGGER.trace("Try to reconnect the counter cluster because cluster client has been degraded for a long time.");
            }
        }
        //判断是否需要降级
        else if (needDegrade()) {
            LOGGER.error("Cluster client has to degrade because counter cluster[" + ip + ":" + port + "] is not available.");
            //注意两个变量的赋值顺序
            degradeStartTimeMs = nowMs;
            serverTempDegraded = true;
            return false;
        }

        return true;
    }

    /**
     * 根据策略判断是否需要降级
     */
    private boolean needDegrade() {
        long failed = failedSlidingWindow.count();
        long total = totalSlidingWindow.count();
        //计算可用率
        double failedRatio = ((double) failed) / total;
        return failedRatio > maxFailedRatio;
    }

    /**
     * 访问失败
     */
    public void failed() {
        totalSlidingWindow.add();
        failedSlidingWindow.add();
    }

    /**
     * 访问成功
     */
    public void succeeded() {
        totalSlidingWindow.add();
        if (serverCrashed) {
            serverCrashed = false;
        }
        if (serverTempDegraded) {
            //注意两个变量的赋值顺序
            serverTempDegraded = false;
            degradeRetryTimes.set(0);
        }
    }

    /**
     * 服务端挂了
     */
    public void serverCrashed() {
        serverCrashed = true;
        LOGGER.error("Cluster counter has crashed.");
    }

    /**
     * 服务端恢复
     */
    public void serverRecovered() {
        serverCrashed = false;
        if (serverTempDegraded) {
            //注意两个变量的赋值顺序
            serverTempDegraded = false;
            degradeRetryTimes.set(0);
        }

        LOGGER.info("Counter cluster[" + this + "] has recovered.");
    }

    /**
     * 是否是相同的节点
     */
    public boolean isSameNode(String ip, Integer port) {
        return Objects.equals(ip, this.ip) && Objects.equals(port, this.port);
    }

    /**
     * 可用率更新,如果minAvailableRatio=null,则表示使用服务自己的可用率
     */
    public void updateAvailableRatio(Double minAvailableRatio) {
        //取消设置，则使用自己的配置
        if (null == minAvailableRatio || minAvailableRatio <= 0 || minAvailableRatio >= 1) {
            minAvailableRatio = serverMinAvailableRatio;
        }
        ValidatorHelper.requireTrue(minAvailableRatio > 0 && minAvailableRatio < 1, "MinAvailableRatio[" + minAvailableRatio + "] is invalid.");
        maxFailedRatio = 1 - minAvailableRatio;
        LOGGER.info("The AvailableRatio of cluster server " + toString() + " has been changed to[" + minAvailableRatio + "].");
    }

    @Override
    public String toString() {
        return "{" + "ip='" + ip + '\'' + ", port=" + port + '}';
    }
}