package com.lifeofcoder.autolimiter.demo.client.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.lifeofcoder.autolimiter.client.rule.RuleChangedListener;
import com.lifeofcoder.autolimiter.common.config.ClusterClientConfig;
import com.lifeofcoder.autolimiter.common.config.ClusterConfigChangedHandler;
import com.lifeofcoder.autolimiter.common.config.ClusterServerConfigItem;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;
import com.lifeofcoder.autolimiter.common.model.RouteMode;
import com.lifeofcoder.autolimiter.common.rule.CustomizedFlowRule;
import com.lifeofcoder.autolimiter.common.utils.SpiFactory;
import com.lifeofcoder.autolimiter.demo.client.Constants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author xbc
 * @date 2020/4/16
 */
@RestController
@RequestMapping("/test")
public class TestController {
    static {
        initRuleAndConfig();
    }

    @SentinelResource(value = "test.aspect", blockHandler = "defAspect")
    @GetMapping("/aspect")
    public String aspect() {
        return "OK";
    }

    public String defAspect(BlockException e) {
        return "Null";
    }

    @GetMapping("/auto")
    public String auto() {
        Entry entry = null;
        try {
            entry = SphU.entry("auto");
        }
        catch (BlockException e) {
            System.out.println("Exception");
        }
        catch (Exception e) {
            Tracer.traceEntry(e, entry);
        }
        finally {
            entry.exit();
        }

        return "auto";
    }

    @GetMapping("/hello")
    public String hello() {
        Entry limiterEntry = null;
        try {
            limiterEntry = SphU.entry(Constants.RES_HELLO);
        }
        catch (BlockException e) {
            return "block";
        }
        catch (Throwable throwable) {
            Tracer.traceEntry(throwable, limiterEntry);
        }
        finally {
            if (limiterEntry != null) {
                limiterEntry.exit();
            }
        }

        return "bocai";
    }

    @GetMapping("/mul")
    public String mul() {
        try {
            Context context = ContextUtil.enter("context1");
            Entry entryA = SphU.entry("A");
            Entry entryB = SphU.entry("B");
            Entry entryC = SphU.entry("C");
            entryC.exit();
            entryB.exit();
            entryA.exit();
            ContextUtil.exit();
        }
        catch (BlockException ex) {
            System.out.println("blocked!");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Context context = ContextUtil.enter("context1");
            Entry entryA = SphU.entry("A");
            Entry entryB2 = SphU.entry("B2");
            Entry entryC2 = SphU.entry("C2");
            entryC2.exit();
            entryB2.exit();
            entryA.exit();
            ContextUtil.exit();
        }
        catch (BlockException ex) {
            System.out.println("blocked!");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Context context = ContextUtil.enter("context2");
            Entry entryA = SphU.entry("A");
            Entry entryB = SphU.entry("B");
            Entry entryC = SphU.entry("C");
            entryC.exit();
            entryB.exit();
            entryA.exit();
            ContextUtil.exit();
        }
        catch (BlockException ex) {
            // 处理被流控的逻辑
            System.out.println("blocked!");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Context context = ContextUtil.enter("context2");
            Entry entryA = SphU.entry("A");
            Entry entryB = SphU.entry("B");
            Entry entryA2 = SphU.entry("A");
            entryA2.exit();
            entryB.exit();
            entryA.exit();
            ContextUtil.exit();
        }
        catch (BlockException ex) {
            // 处理被流控的逻辑
            System.out.println("blocked!");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return "OK";
    }

    private static void initRuleAndConfig() {
        List<FlowRule> rules = new ArrayList<FlowRule>();

        //普通限流设置
        CustomizedFlowRule clusterFlowRule = new CustomizedFlowRule();
        clusterFlowRule.setResource(Constants.RES_HELLO);
        // set limit qps to 20
        clusterFlowRule.setCount(100);
        clusterFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        clusterFlowRule.setLimitApp("default");

        //集群设置
        clusterFlowRule.setClusterMode(true);
        clusterFlowRule.setClusterCount(150D);
        clusterFlowRule.setDynamicCluster(true);
        ClusterFlowConfig clusterFlowConfig = new ClusterFlowConfig();
        clusterFlowConfig.setFlowId(100001L);
        //支持集群总体
        clusterFlowConfig.setThresholdType(1);
        clusterFlowRule.setClusterConfig(clusterFlowConfig);

        rules.add(clusterFlowRule);
        FlowRuleManager.loadRules(rules);

        //通知集群客户端，配置变更
        RuleChangedListener ruleChangedListener = SpiFactory.get(RuleChangedListener.class);
        if (null != ruleChangedListener) {
            ruleChangedListener.ruleChanged(FlowRuleManager.getRules());
        }

        //通知集群topo数据(多个计数节点)
        ClusterConfigChangedHandler clusterConfigChangedHandler = SpiFactory.get(ClusterConfigChangedHandler.class);
        if (null != clusterConfigChangedHandler) {
            /**
             * 基本配置通知
             */
            ClusterClientConfig clientConfig = new ClusterClientConfig();
            //设置路由方式
            clientConfig.setRouteMode(RouteMode.RULE.name());
            clusterConfigChangedHandler.configChanged(ClusterClientConfig.KEY, JSON.toJSONString(clientConfig));

            /**
             * TOPO结构变更通知
             */
            //系统路由计数节点配置
            ClusterServerTopoConfig topoConfig = new ClusterServerTopoConfig();
            topoConfig.setServerHost("127.0.0.1");
            topoConfig.setServerPort(18730);

            //资源路由计数节点配置（可配置多个）
            List<ClusterServerConfigItem> servers = new ArrayList<>();
            ClusterServerConfigItem item = new ClusterServerConfigItem();
            item.setIp("127.0.0.1");
            item.setPort(18730);
            servers.add(item);

//            ClusterServerConfigItem item2 = new ClusterServerConfigItem();
//            item2.setIp("127.0.0.1");
//            item2.setPort(8091);
//            servers.add(item2);

            topoConfig.setServers(servers);

            clusterConfigChangedHandler.configChanged(ClusterServerTopoConfig.KEY, JSON.toJSONString(topoConfig));
        }
    }
}