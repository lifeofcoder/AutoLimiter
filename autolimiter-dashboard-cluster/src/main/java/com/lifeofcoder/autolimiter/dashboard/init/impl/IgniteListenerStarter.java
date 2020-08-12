package com.lifeofcoder.autolimiter.dashboard.init.impl;

import com.lifeofcoder.autolimiter.dashboard.ignite.listener.DiscoveryEventListener;
import com.lifeofcoder.autolimiter.dashboard.init.Starter;
import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Ignite监听启动
 *
 * @author xbc
 * @date 2020/7/29
 */
@Component
public class IgniteListenerStarter implements Starter {
    @Autowired
    private DiscoveryEventListener listener;

    @Autowired
    private Ignite ignite;

    @Override
    public void start() {
        //主动发起本地事件，因为本地机器启动的时候自己收不到自己的启动事件
        ClusterNode clusterNode = ignite.cluster().localNode();
        DiscoveryEvent discoveryEvent = new DiscoveryEvent();
        discoveryEvent.eventNode(clusterNode);
        discoveryEvent.type(EventType.EVT_NODE_JOINED);
        ignite.events().localListen(listener, EventType.EVTS_DISCOVERY);
        listener.apply(discoveryEvent);
    }
}
