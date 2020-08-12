package com.lifeofcoder.autolimiter.dashboard.ignite.listener;

import com.lifeofcoder.autolimiter.dashboard.ignite.node.NodeManger;
import org.apache.ignite.Ignite;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.lang.IgnitePredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 节点发现事件监听
 *
 * @author xbc
 * @date 2020/7/24
 */
@Component
public class DiscoveryEventListener implements IgnitePredicate<DiscoveryEvent>, InitializingBean {
    private final static Logger LOGGER = LoggerFactory.getLogger(DiscoveryEventListener.class);

    private List<NodeChangedListener> nodeChangedLsrList;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Ignite ignite;

    @Override
    public boolean apply(DiscoveryEvent evt) {
        LOGGER.info("DiscoveryEvent[isClient: " + evt.eventNode().isClient() + ", evtType:" + evt.type() + ", node:" + evt.eventNode().addresses() + "] has been caught.");
        if (evt.eventNode().isClient()) {
            return true;
        }

        if (null != ignite) {
            Boolean isMaster = isMasterMode();
            notify4MasterChanged(evt, isMaster);
        }

        return true;
    }

    private Boolean isMasterMode() {
        Boolean isMaster;
        boolean iAmMaster = ignite.cluster().forServers().forOldest().node().isLocal();
        //become master
        if (!NodeManger.isMaster() && iAmMaster) {
            NodeManger.setMasterNode(Boolean.TRUE);
            isMaster = true;
            LOGGER.info("I am the master.");
        }
        //become slave
        else if (NodeManger.isMaster() && !iAmMaster) {
            NodeManger.setMasterNode(Boolean.FALSE);
            isMaster = false;
            LOGGER.info("I becamed a slave.");
        }
        //normal node changed
        else {
            isMaster = null;
        }
        return isMaster;
    }

    private void notify4MasterChanged(DiscoveryEvent evt, Boolean isMaster) {
        nodeChangedLsrList.stream().forEach(l -> l.nodeChanged(evt, isMaster));
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, NodeChangedListener> nodeChangedLsrMap = applicationContext.getBeansOfType(NodeChangedListener.class);
        nodeChangedLsrList = new ArrayList<>(nodeChangedLsrMap.size());
        nodeChangedLsrList.addAll(nodeChangedLsrMap.values());
    }
}