package com.lifeofcoder.autolimiter.dashboard.mapping;

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
public class MappingUtil {
    private static Map<String, FieldWrapper> fieldMap = new HashMap<>();
    private static Object lock = new Object();

    public static FieldWrapper getOrCreate(Class<?> cls) {
        FieldWrapper fieldWrapper = fieldMap.get(cls.getName());
        if (null != fieldWrapper) {
            return fieldWrapper;
        }

        if (null == fieldWrapper) {
            synchronized (lock) {
                if (null == fieldMap.get(cls.getName())) {
                    buildFiledMapping(cls);
                }
            }
        }

        return fieldMap.get(cls.getName());
    }

    private static void buildFiledMapping(Class<?> cls) {
        FieldWrapper fieldWrapper = new FieldWrapper();
        for (Method setter : cls.getMethods()) {
            if (!setter.getName().startsWith("set")) {
                continue;
            }

            String fieldName = toFieldName(setter.getName());
            Field field = null;
            try {
                if ((field = cls.getDeclaredField(fieldName)) == null) {
                    continue;
                }
            }
            catch (NoSuchFieldException e) {
                continue;
            }

            String feildTableName = buildFeildTableName(fieldName);
            fieldWrapper.addFieldMapping(feildTableName, field, setter);
        }
        fieldMap.put(cls.getName(), fieldWrapper);
    }

    private static String toFieldName(String setterMethodName) {
        String tmp = setterMethodName.substring(3);
        return tmp.substring(0, 1).toLowerCase() + tmp.substring(1);
    }

    private static String buildFeildTableName(String fieldName) {
        int start = 0;
        int end = 0;
        StringBuilder nameBuilder = new StringBuilder();
        for (char c : fieldName.toCharArray()) {
            if (Character.isUpperCase(c)) {
                if (nameBuilder.length() != 0) {
                    nameBuilder.append("_");
                }
                nameBuilder.append(fieldName.substring(start, end));
                start = end;
            }
            end++;
        }

        if (nameBuilder.length() != 0) {
            nameBuilder.append("_");
        }
        nameBuilder.append(fieldName.substring(start, end));
        return nameBuilder.toString().toUpperCase();
    }
}
