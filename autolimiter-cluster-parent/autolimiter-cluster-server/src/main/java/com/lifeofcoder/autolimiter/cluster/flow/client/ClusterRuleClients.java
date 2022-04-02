package com.lifeofcoder.autolimiter.cluster.flow.client;

import com.lifeofcoder.autolimiter.cluster.flow.statistic.metric.ClusterMetric;
import com.lifeofcoder.autolimiter.common.link.DlinkNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 集群规则的客户端管理
 *
 * @author xbc
 * @date 2022/3/22
 */
public class ClusterRuleClients {
    private static Logger LOGGER = LoggerFactory.getLogger(ClusterRuleClients.class);

    private static final int MAX_HANDLE_TIME = 64;

    private ClusterMetric clusterMetric;

    /**
     * 最近访问队列，处理并发
     */
    private ConcurrentLinkedQueue<DlinkNode<ClusterClientInfo>> recentQueue;

    /**
     * 双向列表说明：header->a->b->c->d->tail 从a到d是根据最近访问时间排序的。即a是最近访问的，d是最久访问的
     * 客户端双向链表头部
     */
    private DlinkNode<ClusterClientInfo> clientDlinkHeader;

    /**
     * 客户端双向链表尾部
     */
    private DlinkNode<ClusterClientInfo> clientDlinkTail;

    /**
     * Key: IP(long), Value: ClusterClientInfo
     * 记录各个客户端的
     */
    private ConcurrentHashMap<Long, DlinkNode<ClusterClientInfo>> clusterClientInfoMap;

    /**
     * 访问锁
     */
    private ReentrantLock reentrantLock;

    private long flowId;

    public ClusterRuleClients(long flowId, ClusterMetric clusterMetric) {
        reentrantLock = new ReentrantLock();
        clusterClientInfoMap = new ConcurrentHashMap<>();
        clientDlinkHeader = DlinkNode.empty();
        clientDlinkTail = DlinkNode.empty();
        clientDlinkHeader.addNext(clientDlinkTail);
        recentQueue = new ConcurrentLinkedQueue<>();
        this.clusterMetric = clusterMetric;
        this.flowId = flowId;
    }

    /**
     * 访问下客户端(并发接口)
     *
     * @param ip IP
     * @return 返回对应的客户端节点
     */
    public ClusterClientInfo touch(Long ip) {
        DlinkNode<ClusterClientInfo> clusterClientNode = clusterClientInfoMap.computeIfAbsent(ip, k -> new DlinkNode<>(new ClusterClientInfo(ip, clusterMetric, this)));
        clusterClientNode.value().touch();
        recentQueue.add(clusterClientNode);
        return clusterClientNode.value();
    }

    /**
     * 尝试丢弃过期的节点数据，将数据分配的流控值归还给集群
     */
    public void tryDiscard() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Try to remove dead node for rule[" + flowId + "].");
        }
        if (!reentrantLock.tryLock()) {
            return;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Begin to remove dead node for rule[" + flowId + "].");
        }

        try {
            int index = 0;
            DlinkNode<ClusterClientInfo> tmpNode;
            //将recentQueue中的数据替换到队列中。因为占用的业务线程，所以每次最多执行64个，减少对业务线程的影响。
            while ((tmpNode = recentQueue.poll()) != null && ++index < MAX_HANDLE_TIME) {
                clientDlinkHeader.addNext(tmpNode);
            }

            //recentQueue中的数据没有处理完，直接返回，不处理后续死亡节点相关逻辑
            if (index >= MAX_HANDLE_TIME) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The times of moving node to ClientDlink for rule[" + flowId + "] have reached to maximum[" + MAX_HANDLE_TIME + "] ");
                }
                return;
            }

            //处理死亡节点，归还限流值
            index = 0;
            //找到有节点未死亡，即后续节点都是存活状态
            tmpNode = clientDlinkTail.pre();
            while (tmpNode != clientDlinkHeader && tmpNode.value().isDead() && ++index < MAX_HANDLE_TIME) {
                //死亡节点，从链表移除，归还流控值给集群池
                DlinkNode<ClusterClientInfo> removedNode = clusterClientInfoMap.remove(tmpNode.value().getIp());
                if (removedNode != tmpNode) {
                    throw new RuntimeException("The nodes in ClusterClientInfoMap and ClusterClientDlink are different.");
                }

                //先移动指针，否则待会节点删除了，链表就断了
                tmpNode = tmpNode.pre();

                //从链表移除，并将流控值规划给集群池
                removedNode.remove();
                removedNode.value().returnCount();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The node[" + removedNode.value().getIp() + "] of rule[" + flowId + "] has been deleted.");
                }
            }
            tmpNode.addNext(clientDlinkTail);
            if (index >= MAX_HANDLE_TIME && LOGGER.isDebugEnabled()) {
                LOGGER.debug("The times of deleting node for rule[" + flowId + "] have reached to maximum[" + MAX_HANDLE_TIME + "] ");
            }
        }
        finally {
            reentrantLock.unlock();
        }
    }

    /**
     * 获取客户端信息
     */
    public ClientsInfo getClientsInfo() {
        int totalCount = 0, deadClient = 0;
        for (DlinkNode<ClusterClientInfo> node : clusterClientInfoMap.values()) {
            totalCount += node.value().getMaxCount();
            if (node.value().isDead()) {
                deadClient++;
            }
        }

        return new ClientsInfo(clusterClientInfoMap.size(), totalCount, deadClient);
    }
}