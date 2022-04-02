package com.lifeofcoder.autolimiter.common.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 帮助工具类
 *
 * @author xbc
 * @date 2022/4/2
 */
public class ValidatorHelper {
    private ValidatorHelper() {
    }

    public static <T extends Number> T requirePositive(T number, String errorMsg) {
        if (!isPositive(number)) {
            throw new IllegalArgumentException(errorMsg);
        }
        else {
            return number;
        }
    }

    public static Boolean requireTrue(Boolean data, String errorMsg) {
        if (!isTrue(data)) {
            throw new IllegalArgumentException(errorMsg);
        }
        else {
            return data;
        }
    }

    public static boolean requireTrue(boolean data, String errorMsg) {
        if (!data) {
            throw new IllegalArgumentException(errorMsg);
        }
        else {
            return data;
        }
    }

    public static String requireNonEmpty(String str, String errorMsg) {
        if (isEmpty(str)) {
            throw new IllegalArgumentException(errorMsg);
        }
        else {
            return str;
        }
    }

    public static <T extends Collection> T requireNonEmpty(T collection, String errorMsg) {
        if (isEmpty(collection)) {
            throw new IllegalArgumentException(errorMsg);
        }
        else {
            return collection;
        }
    }

    public static <T> T[] requireNonEmpty(T[] array, String errorMsg) {
        if (isEmpty(array)) {
            throw new IllegalArgumentException(errorMsg);
        }
        else {
            return array;
        }
    }

    public static <T> T requireNonEmpty(T array, String errorMsg) {
        if (isEmpty(array)) {
            throw new IllegalArgumentException(errorMsg);
        }
        else {
            return array;
        }
    }

    public static Map requireNonEmpty(Map<?, ?> map, String errorMsg) {
        if (isEmpty(map)) {
            throw new IllegalArgumentException(errorMsg);
        }
        else {
            return map;
        }
    }

    public static <T> T requireNonNull(T obj) {
        return requireNonNull(obj, (Class<T>) obj.getClass());
    }

    public static <T> T requireNonNull(T obj, Class<T> paramClass) {
        return requireNonNull(obj, paramClass.getSimpleName() + " can't be null.");
    }

    public static <T> T requireNonNull(T obj, String errorMsg) {
        if (obj == null) {
            throw new NullPointerException(errorMsg);
        }
        else {
            return obj;
        }
    }

    public static boolean isEmpty(String string) {
        return null == string || string.trim().isEmpty();
    }

    public static boolean isNotEmpty(String string) {
        return !isEmpty(string);
    }

    public static boolean isEmpty(Collection<?> c) {
        return null == c || c.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> c) {
        return !isEmpty(c);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return null == map || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(Object[] array) {
        return null == array || array.length <= 0;
    }

    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    public static boolean isEmpty(Object array) {
        if (null == array) {
            return true;
        }
        else if (!array.getClass().isArray()) {
            throw new RuntimeException("Only array is suppported.");
        }
        else {
            return Array.getLength(array) <= 0;
        }
    }

    public static boolean isNotEmpty(Object array) {
        return !isEmpty(array);
    }

    public static boolean isTrue(Boolean trueOrFalse) {
        return Objects.equals(trueOrFalse, Boolean.TRUE);
    }

    public static boolean isPositive(Number number) {
        if (null == number) {
            return false;
        }
        else if (number instanceof Long) {
            return number.longValue() > 0L;
        }
        else if (number instanceof Integer) {
            return number.intValue() > 0;
        }
        else if (number instanceof Float) {
            return number.floatValue() > 0.0F;
        }
        else if (number instanceof Double) {
            return number.doubleValue() > 0.0D;
        }
        else if (number instanceof Short) {
            return number.shortValue() > 0;
        }
        else if (number instanceof Byte) {
            return number.byteValue() > 0;
        }
        else {
            throw new RuntimeException(number.getClass().getName() + " is not supported.");
        }
    }
}
