package com.lifeofcoder.autolimiter.dashboard.config;

import com.lifeofcoder.autolimiter.dashboard.ResponseDto;

/**
 * 节点配置中心服务，可以将ignite节点信息注册到自己的配置中心
 *
 * @author xbc
 * @date 2020/8/4
 */
public interface NodeConfigService {
    ResponseDto updateNodeAddrs(String addrs);
}
