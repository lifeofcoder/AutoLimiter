package com.lifeofcoder.autolimiter.dashboard.ignite.node;

import com.lifeofcoder.autolimiter.dashboard.JsonUtils;
import com.lifeofcoder.autolimiter.dashboard.ResponseDto;
import com.lifeofcoder.autolimiter.dashboard.config.NodeConfigService;
import com.lifeofcoder.autolimiter.dashboard.config.impl.DefaultNodeConfigService;
import com.lifeofcoder.autolimiter.dashboard.ignite.listener.NodeChangedListener;
import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.events.DiscoveryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 控制台节点注册器，将控制台信息注册到控制中心
 *
 * @author xbc
 * @date 2020/7/24
 */
@Component
public class DashboardNodeRegister implements NodeChangedListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(DashboardNodeRegister.class);

    private AtomicBoolean isUpdating = new AtomicBoolean(Boolean.FALSE);

    @Autowired
    private Ignite ignite;

    private NodeConfigService nodeConfigService = new DefaultNodeConfigService();

    ScheduledExecutorService scheduledExecutorService;

    @Value("${dashboard.register.delay:60}")
    private int registerDelay;

    public DashboardNodeRegister() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void nodeChanged(DiscoveryEvent evt, Boolean isMasterChanged) {
        //如果是主节点，当节点变更的时候就需要重新注册节点信息
        if (NodeManger.isMaster()) {
            tryToRegisterNodeInfo();
        }
        else {
            cancelRegister();
        }
    }

    private void cancelRegister() {
        if (isUpdating.get()) {
            isUpdating.compareAndSet(true, false);
        }
    }

    private void tryToRegisterNodeInfo() {
        if (isUpdating.get()) {
            return;
        }

        if (!isUpdating.compareAndSet(false, true)) {
            return;
        }

        //延迟2分钟执行，是为了应对突发的大规模集群变动，比如重启，或者一起启动
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                if (isUpdating.get()) {
                    doRegisterDashboardNode();
                }
            }
        }, registerDelay, TimeUnit.SECONDS);
    }

    private void doRegisterDashboardNode() {
        StringBuilder addrBuilder = new StringBuilder();
        for (ClusterNode node : ignite.cluster().forServers().nodes()) {
            if (node.isClient()) {
                continue;
            }

            String addr = selectValidAddress(node.addresses());
            if (null != addr) {
                if (addrBuilder.length() == 0) {
                    addrBuilder.append(addr.trim());
                }
                else {
                    addrBuilder.append(",").append(addr.trim());
                }
            }
        }

        LOGGER.debug("Register the dabashboard addrs." + addrBuilder);

        if (addrBuilder.length() == 0) {
            return;
        }

        ResponseDto rsp = nodeConfigService.updateNodeAddrs(addrBuilder.toString());
        if (!ResponseDto.isSucceeded(rsp)) {
            LOGGER.error("Failed to register dabashboard addrs." + JsonUtils.toJson(rsp));
        }
    }

    private String selectValidAddress(Collection<String> addrs) {
        for (String addr : addrs) {
            if (addr.startsWith("0:0:") || addr.startsWith("127.0.0.1")) {
                continue;
            }

            return addr;
        }

        return null;
    }
}
