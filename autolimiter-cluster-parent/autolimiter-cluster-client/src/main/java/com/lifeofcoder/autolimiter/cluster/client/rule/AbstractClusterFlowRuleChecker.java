package com.lifeofcoder.autolimiter.cluster.client.rule;

import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.node.DefaultNode;
import com.alibaba.csp.sentinel.node.Node;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleChecker;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.TrafficShapingController;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.lifeofcoder.autolimiter.cluster.client.config.ClusterClientConfigCenter;
import com.lifeofcoder.autolimiter.cluster.client.consts.ClusterCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群规则检测(抽象类)
 */
public abstract class AbstractClusterFlowRuleChecker extends FlowRuleChecker {
    private final static Logger LOGGER = LoggerFactory.getLogger(AbstractClusterFlowRuleChecker.class);

    /**
     * Controller的Field
     */
    private Field controllerField;

    /**
     * Controller缓存
     */
    private Map<FlowRule, TrafficShapingController> controllerMap = new ConcurrentHashMap<>();

    public AbstractClusterFlowRuleChecker() {
        try {
            controllerField = FlowRule.class.getDeclaredField("controller");
            controllerField.setAccessible(true);
            LOGGER.info(getClass().getSimpleName() + " has been loaded successfully.");
        }
        catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException("Failed to get filed Controller of FlowRule.", e);
        }
    }

    @Override
    public boolean canPassCheck(/*@NonNull*/ FlowRule rule, Context context, DefaultNode node, int acquireCount, boolean prioritized) {
        String limitApp = rule.getLimitApp();
        if (limitApp == null) {
            return true;
        }

        if (ClusterClientConfigCenter.isClusterLimitClosed()) {
            LOGGER.debug("The limiter has falled back to local, because the cluster limiter has been closed.");
            return passLocalCheck(rule, context, node, acquireCount, prioritized);
        }

        if (rule.isClusterMode()) {
            ClusterCheckResult clusterCheckResult = passClusterCheck0(rule, context, node, acquireCount, prioritized);
            if (clusterCheckResult == ClusterCheckResult.PASS) {
                return true;
            }
            else if (clusterCheckResult == ClusterCheckResult.BLOCK) {
                return false;
            }
            else if (clusterCheckResult == ClusterCheckResult.FALLBACK) {
                return fallbackToLocalOrPass(rule, context, node, acquireCount, prioritized);
            }
            else {
                //never happen
                throw new RuntimeException("Illegal clusterCheckResult.");
            }
        }

        return passLocalCheck(rule, context, node, acquireCount, prioritized);
    }

    protected abstract ClusterCheckResult passClusterCheck0(FlowRule rule, Context context, DefaultNode node, int acquireCount, boolean prioritized);

    /**
     * 通过反射获取到Rater
     */
    private TrafficShapingController getRate0(FlowRule rule) {
        return controllerMap.computeIfAbsent(rule, k -> {
            try {
                return (TrafficShapingController) controllerField.get(rule);
            }
            catch (IllegalArgumentException | IllegalAccessException e) {
                throw new RuntimeException("Failed to get filed Controller of FlowRule.", e);
            }
        });
    }

    private boolean passLocalCheck(FlowRule rule, Context context, DefaultNode node, int acquireCount, boolean prioritized) {
        Node selectedNode = selectNodeByRequesterAndStrategy(rule, context, node);
        if (selectedNode == null) {
            return true;
        }
        boolean pass = getRate0(rule).canPass(selectedNode, acquireCount, prioritized);
        if (!pass) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(rule.getResource() + " is blocked by local limiter. pass qps :" + selectedNode.passQps());
            }
        }
        return pass;
    }

    private Node selectReferenceNode(FlowRule rule, Context context, DefaultNode node) {
        String refResource = rule.getRefResource();
        int strategy = rule.getStrategy();

        if (StringUtil.isEmpty(refResource)) {
            return null;
        }

        if (strategy == RuleConstant.STRATEGY_RELATE) {
            return ClusterBuilderSlot.getClusterNode(refResource);
        }

        if (strategy == RuleConstant.STRATEGY_CHAIN) {
            if (!refResource.equals(context.getName())) {
                return null;
            }
            return node;
        }
        // No node.
        return null;
    }

    private boolean filterOrigin(String origin) {
        // Origin cannot be `default` or `other`.
        return !RuleConstant.LIMIT_APP_DEFAULT.equals(origin) && !RuleConstant.LIMIT_APP_OTHER.equals(origin);
    }

    private Node selectNodeByRequesterAndStrategy(/*@NonNull*/ FlowRule rule, Context context, DefaultNode node) {
        // The limit app should not be empty.
        String limitApp = rule.getLimitApp();
        int strategy = rule.getStrategy();
        String origin = context.getOrigin();

        if (limitApp.equals(origin) && filterOrigin(origin)) {
            if (strategy == RuleConstant.STRATEGY_DIRECT) {
                // Matches limit origin, return origin statistic node.
                return context.getOriginNode();
            }

            return selectReferenceNode(rule, context, node);
        }
        else if (RuleConstant.LIMIT_APP_DEFAULT.equals(limitApp)) {
            if (strategy == RuleConstant.STRATEGY_DIRECT) {
                // Return the cluster node.
                return node.getClusterNode();
            }

            return selectReferenceNode(rule, context, node);
        }
        else if (RuleConstant.LIMIT_APP_OTHER.equals(limitApp) && FlowRuleManager.isOtherOrigin(origin, rule.getResource())) {
            if (strategy == RuleConstant.STRATEGY_DIRECT) {
                return context.getOriginNode();
            }

            return selectReferenceNode(rule, context, node);
        }

        return null;
    }

    private boolean fallbackToLocalOrPass(FlowRule rule, Context context, DefaultNode node, int acquireCount, boolean prioritized) {
        if (rule.getClusterConfig().isFallbackToLocalWhenFail()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("[ClusterFlowRuleChecker]FallbackToLocalOrPass[" + rule.getResource() + "] -> " + rule.getCount());
            }
            return passLocalCheck(rule, context, node, acquireCount, prioritized);
        }
        else {
            // The rule won't be activated, just pass.
            return true;
        }
    }
}