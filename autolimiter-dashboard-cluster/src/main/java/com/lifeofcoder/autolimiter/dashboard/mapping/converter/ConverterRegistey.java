package com.lifeofcoder.autolimiter.dashboard.mapping.converter;

import java.util.ArrayList;
import java.util.List;

/**
 * 转换类注册器
 *
 * @author xbc
 * @date 2020/7/23
 */
public class ConverterRegistey {
    private static List<BaseConverter> converterList = new ArrayList<>(2);

    public static void register(BaseConverter converter) {
        converterList.add(converter);
    }

    public static Converter getConverter(Class<?> srcType, Class<?> targetType) {
        for (BaseConverter baseConverter : converterList) {
            if (baseConverter.getSrcType().equals(srcType) && baseConverter.getTargetType().equals(targetType)) {
                return baseConverter;
            }
        }

        return null;
    }
}
