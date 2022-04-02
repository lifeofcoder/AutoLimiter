package com.lifeofcoder.autolimiter.client;

import com.alibaba.csp.sentinel.slotchain.DefaultProcessorSlotChain;
import com.alibaba.csp.sentinel.slotchain.ProcessorSlotChain;
import com.alibaba.csp.sentinel.slotchain.SlotChainBuilder;
import com.alibaba.csp.sentinel.slots.block.authority.AuthoritySlot;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeSlot;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleChecker;
import com.alibaba.csp.sentinel.slots.block.flow.FlowSlot;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import com.alibaba.csp.sentinel.slots.logger.LogSlot;
import com.alibaba.csp.sentinel.slots.nodeselector.NodeSelectorSlot;
import com.alibaba.csp.sentinel.slots.statistic.StatisticSlot;
import com.alibaba.csp.sentinel.slots.system.SystemSlot;
import com.lifeofcoder.autolimiter.common.utils.SpiFactory;

import java.lang.reflect.Field;

/**
 * 自定义的SlotChainBuilder
 *
 * @author xbc
 * @date 2020/4/23
 */
public class CustomizedSlotChainBuilder implements SlotChainBuilder {
    private FlowRuleChecker flowRuleChecker;

    /**
     * 默认构造函数
     */
    public CustomizedSlotChainBuilder() {
        flowRuleChecker = SpiFactory.getOrNull(FlowRuleChecker.class);
        if (null == flowRuleChecker) {
            flowRuleChecker = new FlowRuleChecker();
        }
    }

    @Override
    public ProcessorSlotChain build() {
        ProcessorSlotChain chain = new DefaultProcessorSlotChain();
        chain.addLast(new NodeSelectorSlot());
        chain.addLast(new ClusterBuilderSlot());
        chain.addLast(new LogSlot());
        chain.addLast(new StatisticSlot());

        //        chain.addLast(new ParamFlowSlot());
        chain.addLast(new SystemSlot());
        chain.addLast(new AuthoritySlot());
        chain.addLast(buildFlowSlot());
        chain.addLast(new DegradeSlot());

        return chain;
    }

    /**
     * 通过反射构建FlowSlot
     */
    private FlowSlot buildFlowSlot() {
        try {
            FlowSlot flowSlot = new FlowSlot();
            Field checkerField = FlowSlot.class.getDeclaredField("checker");
            checkerField.setAccessible(true);
            //集群限流可以自定义ClusterFlowRuleChecker
            checkerField.set(flowSlot, flowRuleChecker);
            return flowSlot;
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to get checker field of FlowSlot.", e);
        }
        catch (IllegalAccessException e) {
            throw  new RuntimeException("Failed to set checker field for FlowSlot.", e);
        }
    }
}
