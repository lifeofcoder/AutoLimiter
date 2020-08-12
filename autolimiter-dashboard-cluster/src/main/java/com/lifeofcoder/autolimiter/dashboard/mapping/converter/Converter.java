package com.lifeofcoder.autolimiter.dashboard.mapping.converter;

/**
 * 格式转换工具
 *
 * @author xbc
 * @date 2020/7/23
 */
public interface Converter {
    /**
     * 进行格式转换
     * @param src 源对象
     * @return 目标对象
     */
    Object convert(Object src);
}
