package com.lifeofcoder.autolimiter.common.config;

/**
 * 配置变更处理类
 *
 * @author xbc
 * @date 2022/4/2
 */
public interface ClusterConfigChangedHandler {
    void configChanged(String key, String config);
}