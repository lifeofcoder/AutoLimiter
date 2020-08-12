package com.lifeofcoder.autolimiter.dashboard.ignite.listener;

import org.apache.ignite.events.DiscoveryEvent;

/**
 * 服务端节点变更通知（不包括客户端变更）
 *
 * @author xbc
 * @date 2020/7/24
 */
public interface NodeChangedListener {
    /**
     * 只要有节点变化就会通知.
     * 如果isMasterChanged不为空，则表示是普通的节点变更。
     * 如果isMasterChanged不为空，则涉及到节点master节点变更。isMaster=true表示当前节点变成了master节点，
     * 如果isMasterChanged=fasle，则表示当前节点从master节点变成了普通节点
     */
    void nodeChanged(DiscoveryEvent evt, Boolean isMasterChanged);
}
