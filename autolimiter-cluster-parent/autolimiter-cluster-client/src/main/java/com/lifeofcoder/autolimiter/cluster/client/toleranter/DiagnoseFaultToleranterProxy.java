package com.lifeofcoder.autolimiter.cluster.client.toleranter;

import com.alibaba.csp.sentinel.cluster.TokenServerDescriptor;
import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.common.config.ClusterClientConfig;
import com.lifeofcoder.autolimiter.common.config.ClusterServerTopoConfig;
import com.lifeofcoder.autolimiter.common.model.RouteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 诊断容错器，负责对Counter集群进行诊断与容错代理类
 * 动态代理：选择System路由或者Resource路由
 *
 * @author xbc
 * @date 2021/7/2
 */
public class DiagnoseFaultToleranterProxy {
    private final static Logger LOGGER = LoggerFactory.getLogger(DiagnoseFaultToleranterProxy.class);

    private static volatile FaultToleranterManager faultToleranterManager;

    private static volatile RouteMode routeMode;

    private static volatile Double availableRatio;

    /**
     * 查找对应的容错策略
     */
    public static DiagnoseFaultToleranter getDiagnoseFaultToleranter(TokenServerDescriptor tokenServerDescriptor) {
        FaultToleranterManager tmpFaultToleranterManager = faultToleranterManager;
        if (null == tmpFaultToleranterManager) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("There is no FaultToleranterManager for Node " + tokenServerDescriptor);
            }
            return null;
        }

        return tmpFaultToleranterManager.selectDiagnoseFaultToleranter(tokenServerDescriptor.getHost(), tokenServerDescriptor.getPort());
    }

    /**
     * 是否可以访问
     */
    public static boolean canAccess(DiagnoseFaultToleranter diagnoseFaultToleranter) {
        if (null == diagnoseFaultToleranter) {
            return false;
        }

        boolean canAccess = diagnoseFaultToleranter.canAccess();
        if (!canAccess && LOGGER.isDebugEnabled()) {
            LOGGER.debug("The request is blocked. RouteMode: " + routeMode);
        }
        return canAccess;
    }

    /**
     * 访问失败
     */
    public static void failed(DiagnoseFaultToleranter diagnoseFaultToleranter) {
        if (null == diagnoseFaultToleranter) {
            return;
        }

        diagnoseFaultToleranter.failed();
    }

    /**
     * 访问成功
     */
    public static void succeeded(DiagnoseFaultToleranter diagnoseFaultToleranter) {
        if (null == diagnoseFaultToleranter) {
            return;
        }

        diagnoseFaultToleranter.succeeded();
    }

    /**
     * 配置变更(路由，可用率)
     */
    public synchronized static void clusterClientConfigChanged(ClusterClientConfig clusterClientConfig) {
        if (null == clusterClientConfig) {
            LOGGER.error("Failed to get ClusterServerTopoConfig.");
            return;
        }

        RouteMode newRouteMode = clusterClientConfig.getRouteMode();
        RouteMode oldRouteMode = routeMode;

        ClusterServerTopoConfig topoConfig = ClusterClientConfigManager.currentClusterServerTopoConfig();

        //路由模式切换
        if (!Objects.equals(newRouteMode, oldRouteMode)) {
            routeModeChanged(newRouteMode, topoConfig);
        }

        //可用率变更
        if (!Objects.equals(availableRatio, clusterClientConfig.getAvailableRatio())) {
            FaultToleranterManager tmpFaultToleranterManager = faultToleranterManager;
            if (null != tmpFaultToleranterManager) {
                tmpFaultToleranterManager.availableRatioChanged(clusterClientConfig.getAvailableRatio());
            }
            Double oldAvailableRatio = availableRatio;
            availableRatio = clusterClientConfig.getAvailableRatio();
            LOGGER.info("The AvailableRatio has been changed to [" + availableRatio + "] from[" + oldAvailableRatio + "].");
        }

        LOGGER.info("The ClusterClientConfig have been changed. The current config is [" + clusterClientConfig + "].");
    }

    private static void routeModeChanged(RouteMode newRouteMode, ClusterServerTopoConfig topoConfig) {
        FaultToleranterManager newFaultToleranterManager;
        //切换成系统模式
        if (RouteMode.SYSTEM == newRouteMode) {
            newFaultToleranterManager = new SystemFaultToleranterManager();
        }
        //切换成规则模式
        else {
            newFaultToleranterManager = new RuleFaultToleranterManager();
        }
        newFaultToleranterManager.clusterServerTopoConfigChanged(topoConfig);

        faultToleranterManager = newFaultToleranterManager;
        RouteMode oldRouteMode = routeMode;

        //数据已经设置好，切换路由模式
        routeMode = newRouteMode;
        LOGGER.info("The route mode is changed to [" + newRouteMode + "] from [" + oldRouteMode + "].");
    }

    /**
     * Topo结构变更
     * 不允许并发
     */
    public synchronized static void clusterServerTopoChanged(ClusterServerTopoConfig entity) {
        FaultToleranterManager tmpFaultToleranterManager = faultToleranterManager;
        if (null != tmpFaultToleranterManager) {
            tmpFaultToleranterManager.clusterServerTopoConfigChanged(entity);
            LOGGER.info("The ClusterServerTopoConfig has been changed.");
        }
        //没有，可能没有初始化，这要帮忙初始化下。否则可能一直都没有faultToleranterManager
        //因为faultToleranterManager的初始化依赖ClusterClientConfig变更
        else {
            routeModeChanged(ClusterClientConfigManager.currentRouteMode(), entity);
        }
    }
}