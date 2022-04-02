package com.lifeofcoder.autolimiter.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 *
 * @author xbc
 * @date 2022/4/2
 */
public class SpiFactory {
    private static Logger LOGGER = LoggerFactory.getLogger(SpiFactory.class);
    private static final Map<String, List<?>> INSTANCES_MAP = new HashMap();

    private SpiFactory() {
    }

    public static <T> T get(Class<T> type) throws RuntimeException {
        T instance = doGet(type);
        if (null == instance) {
            throw new RuntimeException("Failed to get instance for class[" + type.getName() + "].");
        } else {
            return instance;
        }
    }

    public static <T> T getOrNull(Class<T> type) {
        return getOrDefault(type, (T)null);
    }

    public static <T> T getOrDefault(Class<T> type, T defaultInstance) {
        T instance = doGet(type);
        if (null == instance) {
            instance = defaultInstance;
        }

        return instance;
    }

    private static <T> T doGet(Class<T> type) {
        List<T> instanceList = list(type);
        return null == instanceList || instanceList.isEmpty() ? null : instanceList.get(0);
    }

    public static <T> List<T> list(Class<T> type) {
        return listInstanceWithOrder(type);
    }

    private static <T> List<T> listInstanceWithOrder(Class<T> type) {
        List<T> instanceList = (List)INSTANCES_MAP.get(type.getName());
        if (null == instanceList) {
            instanceList = doLoadInstancesWithOrder(type);
        }

        if (instanceList.isEmpty()) {
            LOGGER.info("There is no instance for type[" + type.getName() + "].");
            return null;
        } else {
            List<T> destInstanceList = new ArrayList(instanceList.size());
            Iterator<T> var3 = instanceList.iterator();

            while(var3.hasNext()) {
                T instance = var3.next();
                destInstanceList.add(instance);
            }

            return destInstanceList;
        }
    }

    private static synchronized <T> List<T> doLoadInstancesWithOrder(Class<T> type) {
        List<T> instanceList = load(type, Thread.currentThread().getContextClassLoader());
        if ((null == instanceList || instanceList.isEmpty())
            && Thread.currentThread().getContextClassLoader() != type.getClassLoader()) {
            instanceList = load(type, type.getClassLoader());
        }

        if (null == instanceList) {
            instanceList = new ArrayList(0);
        }

//        OrderHepler.sort((List)instanceList);
        INSTANCES_MAP.put(type.getName(), instanceList);
        return (List)instanceList;
    }

    private static <T> List<T> load(Class<T> type, ClassLoader classLoader) {
        List<T> instanceList = new ArrayList(2);
        ServiceLoader<T> load = ServiceLoader.load(type, classLoader);
        Iterator<T> var4 = load.iterator();

        while(var4.hasNext()) {
            T isntance = var4.next();
            instanceList.add(isntance);
        }

        return instanceList;
    }
}
