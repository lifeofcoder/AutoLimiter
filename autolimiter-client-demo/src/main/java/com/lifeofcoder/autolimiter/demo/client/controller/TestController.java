package com.lifeofcoder.autolimiter.demo.client.controller;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
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
        initRule();
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
            limiterEntry.exit();
        }

        return "bocai";
    }

    @GetMapping("/mul")
    public String mul() {
        System.out.println("AAAAAAAAAdDDDDDDDDDDASSS");
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

    private static void initRule() {
        List<FlowRule> rules = new ArrayList<FlowRule>();
        FlowRule rule1 = new FlowRule();
        rule1.setResource(Constants.RES_HELLO);
        // set limit qps to 20
        rule1.setCount(20);
        rule1.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule1.setLimitApp("default");
        rules.add(rule1);
        FlowRuleManager.loadRules(rules);

        CacheBuilder.<String, String>newBuilder().maximumSize(10);

        CacheBuilder.<String, String>newBuilder().maximumWeight(10).weigher(new Weigher<String, String>() {
            @Override
            public int weigh(String key, String value) {
                //通过权重，返回缓存项的字节大小，从而实现容量限制
                return 0;
            }
        });
    }
}
