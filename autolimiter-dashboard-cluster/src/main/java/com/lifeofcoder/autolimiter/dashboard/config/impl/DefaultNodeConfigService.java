package com.lifeofcoder.autolimiter.dashboard.config.impl;

import com.lifeofcoder.autolimiter.dashboard.ResponseDto;
import com.lifeofcoder.autolimiter.dashboard.config.NodeConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认实现，什么也不做
 *
 * @author xbc
 * @date 2020/8/4
 */
public class DefaultNodeConfigService implements NodeConfigService {
    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultNodeConfigService.class);

    @Override
    public ResponseDto updateNodeAddrs(String addrs) {
        LOGGER.info("注册Ignite节点信息到配置中心");
        return ResponseDto.SUCCESS;
    }
}
