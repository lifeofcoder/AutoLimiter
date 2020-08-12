/**
 *
 */
package com.lifeofcoder.autolimiter.dashboard;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Json工具类
 * @author xbc
 */
public class JsonUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    public static <T> T parseObject(String json, Class<T> cls) {
        return JSON.parseObject(json, cls);
    }

    public static <T> T parseObject(String json, TypeReference<T> cls) {
        return JSON.parseObject(json, cls);
    }

    public static <T> List<T> parseArray(String json, Class<T> tClass) {
        return JSON.parseArray(json, tClass);
    }

    /**
     * 将对象转换成json 字符串
     * @param obj
     * @return
     */
    public static String toJson(Object obj) {
        return JSON.toJSONString(obj);
    }

}

