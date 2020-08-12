package com.lifeofcoder.autolimiter.dashboard.mapping.converter.impl;

import com.lifeofcoder.autolimiter.dashboard.mapping.converter.BaseConverter;

import java.util.Date;

/**
 * Long转换为时间
 * 用sql的方式查询出来的都是对象类型，而不会是long，所以直接用Long转Date即可
 *
 * @author xbc
 * @date 2020/7/23
 */
public class Long2DateConverter extends BaseConverter<Long, Date> {
    public Long2DateConverter() {
        super(Long.class, Date.class);
    }

    @Override
    public Date doConvert(Long srcObject) {
        if (null == srcObject) {
            return null;
        }

        return new Date(srcObject.longValue());
    }
}