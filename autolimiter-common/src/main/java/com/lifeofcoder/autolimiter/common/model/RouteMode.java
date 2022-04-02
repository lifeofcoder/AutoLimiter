package com.lifeofcoder.autolimiter.common.model;

import com.alibaba.csp.sentinel.util.StringUtil;

import java.util.Objects;

/**
 * 路由模式枚举
 *
 * @author xbc
 * @date 2022/3/15
 */
public enum RouteMode {
    SYSTEM("system"), RULE("rule");

    /**
     * 路由模式
     */
    private String mode;

    RouteMode(String mode) {
        this.mode = mode;
    }

    public static RouteMode of(String mode) {
        if (StringUtil.isEmpty(mode)) {
            return SYSTEM;
        }

        for (RouteMode value : values()) {
            if (Objects.equals(value.mode, mode)) {
                return value;
            }
        }

        return SYSTEM;
    }
}
