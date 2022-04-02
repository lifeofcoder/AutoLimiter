package com.lifeofcoder.autolimiter.cluster.client.dynamic;

import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.cluster.client.TokenClientProvider;
import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import com.lifeofcoder.autolimiter.cluster.client.ChangeableClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.client.toleranter.DiagnoseFaultToleranter;
import com.lifeofcoder.autolimiter.cluster.client.toleranter.DiagnoseFaultToleranterProxy;
import com.lifeofcoder.autolimiter.cluster.common.result.DynamicTokenResult;
import com.lifeofcoder.autolimiter.common.link.DlinkNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 集群流控更新时间轮
 *
 * @author xbc
 * @date 2022/3/17
 */
public class ClusterFlowUpdateWheelTimer {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterFlowUpdateWheelTimer.class);

    private static final int SEGMENT_NUM = 10;

    /**
     * 将一秒钟分为10份，所有的DynamicFlowCounterNode随机分配到节点上
     * 后续没100ms跳转一格，并处理其下的所有节点更新
     */
    private DlinkNode<DynamicFlowCounter>[] dynamicFlowCounterNodes;

    private volatile Thread coreThread;

    private int currentWheelIndex;

    private ExecutorService executorService;

    public ClusterFlowUpdateWheelTimer() {
        //初始化链表
        dynamicFlowCounterNodes = new DlinkNode[SEGMENT_NUM];
        for (int i = 0; i < SEGMENT_NUM; i++) {
            dynamicFlowCounterNodes[i] = DlinkNode.empty();
        }

        int max = Runtime.getRuntime().availableProcessors();
        executorService = new ThreadPoolExecutor(max * 3, max * 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new NamedThreadFactory("Cluster-Flow-Wheel-Timer"));
    }

    private void lazyStart() {
        if (coreThread != null) {
            return;
        }

        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("ClusterFlowUpdateWheelTimer", true);
        coreThread = namedThreadFactory.newThread(new Runnable() {
            @Override
            public void run() {
                runWheel();
            }
        });
        coreThread.start();
    }

    /**
     * 执行时间轮
     */
    private void runWheel() {
        while (true) {
            try {
                TimeUnit.MILLISECONDS.sleep(1000 / SEGMENT_NUM);
                DlinkNode<DynamicFlowCounter> rootNode = dynamicFlowCounterNodes[currentWheelIndex % SEGMENT_NUM];
                execute(rootNode);
                //放置移除
                if (++currentWheelIndex == Integer.MAX_VALUE) {
                    currentWheelIndex = 0;
                }
            }
            catch (InterruptedException e) {
                //ignore
            }
            //业务异常不能导致循环中断
            catch (Exception e) {
            }
        }
    }

    /**
     * 执行更新，同时删除无效节点
     */
    private void execute(DlinkNode<DynamicFlowCounter> rootNode) {
        DlinkNode<DynamicFlowCounter> nextNode = rootNode.next();
        while (nextNode != null) {
            DlinkNode<DynamicFlowCounter> currentNode = nextNode;
            nextNode = nextNode.next();
            //被删除了，从链表中移除
            if (currentNode.value().isDeleted()) {
                currentNode.remove();
            }
            else {
                executorService.submit(() -> doUpdateCountFromCluster(currentNode.value()));
            }
        }
    }

    /**
     * 更新集群限流值
     */
    private void doUpdateCountFromCluster(DynamicFlowCounter dynamicFlowCounter) {
        TokenService clusterService = TokenClientProvider.getClient();
        if (!(clusterService instanceof ChangeableClusterTokenClient)) {
            throw new RuntimeException("Token server must be a ChangeableClusterTokenClient.");
        }

        long flowId = dynamicFlowCounter.getFlowId();
        DynamicClusterTokenClient selectedClient = ((ChangeableClusterTokenClient) clusterService).selectClusterTokenClient(flowId);
        if (selectedClient == null) {
            LOGGER.error("There is no valid cluster token client for [" + flowId + "].");
            return;
        }

        DiagnoseFaultToleranter diagnoseFaultToleranter = DiagnoseFaultToleranterProxy.getDiagnoseFaultToleranter(selectedClient.currentServer());
        try {
            if (!DiagnoseFaultToleranterProxy.canAccess(diagnoseFaultToleranter)) {
                dynamicFlowCounter.clusterCrashed();
                LOGGER.debug("The cluster counter has been crashed.");
                return;
            }

            //这里获取总的QPS, 而没有获取PassQPS是因为，计算负载流量应该根据所有访问量算
            int lastQps = dynamicFlowCounter.getCurrentTotalCount();

            //请求服务端
            DynamicTokenResult result = selectedClient.requestDynamicToken(flowId, dynamicFlowCounter.getMaxCount(), lastQps);

            //集群节点请求成功才算成功
            if (null != result && null != result.getStatus() && result.getStatus() >= TokenResultStatus.OK) {
                DiagnoseFaultToleranterProxy.succeeded(diagnoseFaultToleranter);
                if (result.getStatus() == TokenResultStatus.OK) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("The new flow counter info of flow[" + flowId + "] is [maxCount:" + result.getCount() + ", lastQPS:" + lastQps + ", waitInMs:" + result.getWaitInMs() + "].");
                    }
                    dynamicFlowCounter.updateMaxCount(result.getCount());
                }
            }
            else {
                dynamicFlowCounter.clusterCrashed();
                DiagnoseFaultToleranterProxy.failed(diagnoseFaultToleranter);
            }
        }
        catch (Throwable ex) {
            LOGGER.error("[ClusterFlowRuleChecker] Request cluster token unexpected failed", ex);
            dynamicFlowCounter.clusterCrashed();
            DiagnoseFaultToleranterProxy.failed(diagnoseFaultToleranter);
        }
    }

    /**
     * 将新的数据添加到链表
     */
    public synchronized void addDynamicFlowCounters(List<DynamicFlowCounter> newDynamicFlowCounterList) {
        if (newDynamicFlowCounterList == null || newDynamicFlowCounterList.isEmpty()) {
            return;
        }

        SecureRandom secureRandom = new SecureRandom();
        int index;
        for (DynamicFlowCounter dynamicFlowCounter : newDynamicFlowCounterList) {
            index = secureRandom.nextInt(SEGMENT_NUM);
            dynamicFlowCounterNodes[index].addNext(new DlinkNode<>(dynamicFlowCounter));
        }

        lazyStart();
    }
}
