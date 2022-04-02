package com.lifeofcoder.autolimiter.cluster.client.dynamic;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.fastjson.JSON;
import com.lifeofcoder.autolimiter.common.utils.ValidatorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态流控计数器管理类
 *
 * @author xbc
 * @date 2022/3/17
 */
public class DynamicFlowCounterManager {
    private static Logger LOGGER = LoggerFactory.getLogger(DynamicFlowCounterManager.class);

    private static volatile Map<Long, DynamicFlowCounter> dynamicFlowCounterMap = new HashMap<>();

    private static ClusterFlowUpdateWheelTimer clusterFlowUpdateWheelTimer = new ClusterFlowUpdateWheelTimer();

    /**
     * 规则变更，可能需要变更map
     */
    public synchronized static void ruleChanged(List<FlowRule> ruleList) {
        if (ValidatorHelper.isEmpty(ruleList)) {
            if (ValidatorHelper.isNotEmpty(dynamicFlowCounterMap)) {
                dynamicFlowCounterMap = new HashMap<>();
            }
            return;
        }

        Map<Long, DynamicFlowCounter> newDynamicFlowCounterMap = new HashMap<>();
        Map<Long, DynamicFlowCounter> oldDynamicFlowCounterMap = dynamicFlowCounterMap;
        DynamicFlowCounter dynamicFlowCounter;
        Long flowId;
        List<DynamicFlowCounter> newDynamicFlowCounterList = new ArrayList<>();
        for (FlowRule flowRule : ruleList) {
            flowId = flowRule.getClusterConfig().getFlowId();
            dynamicFlowCounter = oldDynamicFlowCounterMap.get(flowId);
            if (null == dynamicFlowCounter) {
                dynamicFlowCounter = new DynamicFlowCounter(flowId, 0);
                newDynamicFlowCounterList.add(dynamicFlowCounter);
            }
            newDynamicFlowCounterMap.put(flowId, dynamicFlowCounter);
        }

        //如果没有新增任何flowId, 且新旧的数量相同，则说明整个flow规则没有任何改变
        boolean notChanged = newDynamicFlowCounterList.isEmpty() && newDynamicFlowCounterMap.size() == oldDynamicFlowCounterMap.size();
        if (notChanged) {
            LOGGER.info("The cluster flow rules have not been changed.");
            return;
        }

        //标记删除无效节点，延迟删除：等待列表扫描的时候再删除(从链表中移除)
        for (Map.Entry<Long, DynamicFlowCounter> entry : oldDynamicFlowCounterMap.entrySet()) {
            if (!newDynamicFlowCounterMap.containsKey(entry.getKey())) {
                entry.getValue().delete();
            }
        }

        //将新counter添加到链表中
        clusterFlowUpdateWheelTimer.addDynamicFlowCounters(newDynamicFlowCounterList);

        dynamicFlowCounterMap = newDynamicFlowCounterMap;
        LOGGER.info("The cluster flow rules have been changed." + JSON.toJSONString(newDynamicFlowCounterMap.keySet()));
    }

    public static DynamicFlowCounter getDynamicFlowCounter(long flowId) {
        Map<Long, DynamicFlowCounter> tmpDynamicFlowCounterMap = dynamicFlowCounterMap;
        if (ValidatorHelper.isEmpty(tmpDynamicFlowCounterMap)) {
            return null;
        }

        return tmpDynamicFlowCounterMap.get(flowId);
    }
}