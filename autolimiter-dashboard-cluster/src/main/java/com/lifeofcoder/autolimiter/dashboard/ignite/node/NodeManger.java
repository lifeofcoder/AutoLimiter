package com.lifeofcoder.autolimiter.dashboard.ignite.node;

/**
 * 节点管理
 *
 * @author xbc
 * @date 2020/7/30
 */
public class NodeManger {
    private static volatile Boolean isMasterNode;

    public static void setMasterNode(Boolean isMaster) {
        isMasterNode = isMaster;
    }

    public static boolean isMaster() {
        return null != isMasterNode && isMasterNode;
    }
}
