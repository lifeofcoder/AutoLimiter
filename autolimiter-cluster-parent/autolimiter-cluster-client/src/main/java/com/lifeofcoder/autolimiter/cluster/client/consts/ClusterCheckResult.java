package com.lifeofcoder.autolimiter.cluster.client.consts;

/**
 * 集群结果
 *
 * @author xbc
 * @date 2022/3/17
 */
public enum ClusterCheckResult {
    PASS, //通过
    BLOCK, //阻塞
    FALLBACK; //降级
}
