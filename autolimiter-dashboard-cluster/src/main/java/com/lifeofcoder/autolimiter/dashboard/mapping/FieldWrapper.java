package com.lifeofcoder.autolimiter.dashboard.mapping;

import com.lifeofcoder.autolimiter.dashboard.mapping.converter.Converter;
import com.lifeofcoder.autolimiter.dashboard.mapping.converter.ConverterRegistey;
import com.lifeofcoder.autolimiter.dashboard.mapping.converter.impl.Long2DateConverter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 *
 * @author xbc
 * @date 2020/7/21
 */
public class FieldWrapper {
    Map<String, FieldMapping> fieldMappingMap = new HashMap<>(4);

    static {
        ConverterRegistey.register(new Long2DateConverter());
    }

    void addFieldMapping(String tableFieldName, Field field, Method setter) {
        fieldMappingMap.put(tableFieldName, new FieldMapping(field, setter));
    }

    public void set(Object obj, String tableFileName, Object value) {
        FieldMapping fieldMapping = fieldMappingMap.get(tableFileName);
        if (null == fieldMapping) {
            return;
        }

        if (null == value) {
            return;
        }

        Object tmpValue = value;
        Converter converter = ConverterRegistey.getConverter(value.getClass(), fieldMapping.getField().getType());
        if (null != converter) {
            tmpValue = converter.convert(value);
        }

        try {
            fieldMapping.getSetter().invoke(obj, tmpValue);
        }
        catch (Exception e) {
            String errorMsg = "Filed name : " + fieldMapping.getField().getName() + ", table field name : " + tableFileName + ", srctype :" + value.getClass().getName() + ", targetType:" + fieldMapping.getField().getType().getName();
            throw new RuntimeException(errorMsg, e);
        }
    }
}
