package com.lifeofcoder.autolimiter.cluster.client.rule;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.cluster.client.ClusterTokenClient;
import com.alibaba.csp.sentinel.cluster.client.TokenClientProvider;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.lifeofcoder.autolimiter.client.AutoLimiterException;
import com.lifeofcoder.autolimiter.cluster.client.ChangeableClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.client.consts.ClusterCheckResult;
import com.lifeofcoder.autolimiter.cluster.client.toleranter.DiagnoseFaultToleranter;
import com.lifeofcoder.autolimiter.cluster.client.toleranter.DiagnoseFaultToleranterProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群规则检测
 */
public class ClusterFlowRuleChecker extends AbstractClusterFlowRuleChecker {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterFlowRuleChecker.class);

    @Override
    protected ClusterCheckResult passClusterCheck0(FlowRule rule, Context context, DefaultNode node, int acquireCount, boolean prioritized) {
        TokenService clusterService = pickClusterService();
        if (!(clusterService instanceof ChangeableClusterTokenClient)) {
            throw new AutoLimiterException("Token server must be a ChangeableClusterTokenClient.");
        }

        long flowId = rule.getClusterConfig().getFlowId();
        ClusterTokenClient selectedClient = ((ChangeableClusterTokenClient) clusterService).selectClusterTokenClient(flowId);
        if (selectedClient == null) {
            LOGGER.error("There is no valid cluster token client for [" + flowId + "].");
            return ClusterCheckResult.FALLBACK;
        }

        DiagnoseFaultToleranter diagnoseFaultToleranter = DiagnoseFaultToleranterProxy.getDiagnoseFaultToleranter(selectedClient.currentServer());
        try {
            if (!DiagnoseFaultToleranterProxy.canAccess(diagnoseFaultToleranter)) {
                LOGGER.debug("The cluster counter has been crashed.");
                return ClusterCheckResult.FALLBACK;
            }

            //请求服务端
            TokenResult result = selectedClient.requestToken(flowId, acquireCount, prioritized);

            //集群节点请求成功才算成功
            if (null != result && null != result.getStatus() && result.getStatus() >= TokenResultStatus.OK) {
                DiagnoseFaultToleranterProxy.succeeded(diagnoseFaultToleranter);
            }
            else {
                DiagnoseFaultToleranterProxy.failed(diagnoseFaultToleranter);
            }

            //处理结果
            return applyTokenResult(result, rule, context, node, acquireCount, prioritized);
        }
        catch (Throwable ex) {
            LOGGER.error("[ClusterFlowRuleChecker] Request cluster token unexpected failed", ex);
            DiagnoseFaultToleranterProxy.failed(diagnoseFaultToleranter);
        }

        return ClusterCheckResult.FALLBACK;
    }

    /**
     * 通过SPI选择ClusterService
     */
    private TokenService pickClusterService() {
        return TokenClientProvider.getClient();
    }

    private ClusterCheckResult applyTokenResult(/*@NonNull*/ TokenResult result, FlowRule rule, Context context, DefaultNode node, int acquireCount, boolean prioritized) {
        if (result.getStatus() != TokenResultStatus.OK && result.getStatus() != TokenResultStatus.BLOCKED) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[ClusterFlowRuleChecker]Failed to request counter cluster[response status : " + result.getStatus() + "] for resource[" + rule.getResource() + "].");
            }
        }
        switch (result.getStatus()) {
            case TokenResultStatus.OK:
                return ClusterCheckResult.PASS;
            case TokenResultStatus.SHOULD_WAIT:
                // Wait for next tick.
                try {
                    Thread.sleep(result.getWaitInMs());
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return ClusterCheckResult.PASS;
            case TokenResultStatus.NO_RULE_EXISTS:
            case TokenResultStatus.BAD_REQUEST:
            case TokenResultStatus.FAIL:
                //            case TokenResultStatus.TOO_MANY_REQUEST:
                //                return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
                //降不降级由配置说了算，不要代码处理 ，TOO_MANY_REQUEST意味着集群Counter单机超过最大请求量
            case TokenResultStatus.TOO_MANY_REQUEST:
                return ClusterCheckResult.FALLBACK;

            case TokenResultStatus.BLOCKED:
            default:
                return ClusterCheckResult.BLOCK;
        }
    }
}