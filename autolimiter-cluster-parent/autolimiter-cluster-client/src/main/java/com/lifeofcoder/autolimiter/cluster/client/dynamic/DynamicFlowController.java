package com.lifeofcoder.autolimiter.cluster.client.dynamic;

import com.lifeofcoder.autolimiter.cluster.client.consts.ClusterCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动态流控控制器
 * 动态集群流控策略：
 * 1、所有机器流控动态分配到单机上执行单机流控
 * 2、客户端每一秒发送一次请求到集群节点去获取最新的流控信息
 *
 * @author xbc
 * @date 2022/3/17
 */
public class DynamicFlowController {
    private final static Logger LOGGER = LoggerFactory.getLogger(DynamicFlowController.class);

    /**
     * 检测集群流控结果
     */
    public ClusterCheckResult clusterFlowCheck(Long flowId) {
        if (null == flowId) {
            LOGGER.debug("The request is blocked because the Flow Id is null.");
            return ClusterCheckResult.BLOCK;
        }

        DynamicFlowCounter dynamicFlowCounter = DynamicFlowCounterManager.getDynamicFlowCounter(flowId);
        if (null == dynamicFlowCounter) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The request is fallback because the is no DynamicFlowCounter for [" + flowId + "].");
            }
            return ClusterCheckResult.FALLBACK;
        }

        return dynamicFlowCounter.canPass();
    }
}