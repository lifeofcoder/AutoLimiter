package com.lifeofcoder.autolimiter.cluster.client.consts;

/**
 * 公共参数
 *
 * @author xbc
 * @date 2022/3/9
 */
public final class CommonParams {
    /**
     * 客户端延迟关闭时长
     */
    public static final long CLOSE_CLIENT_DELAY_SEC = 30;

    /**
     * 默认最小可用率
     */
    public static final double DEF_MIN_AVAILABLE_RATIO = 0.95D;
}
