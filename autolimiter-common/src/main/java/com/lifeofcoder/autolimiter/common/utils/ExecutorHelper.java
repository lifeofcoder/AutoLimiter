package com.lifeofcoder.autolimiter.common.utils;

import com.alibaba.csp.sentinel.concurrent.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 执行器工具
 *
 * @author xbc
 * @date 2022/4/2
 */
public class ExecutorHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorHelper.class);
    private static final ScheduledExecutorService scheduledExecutorService;
    private static final ExecutorService executorService;

    private ExecutorHelper() {
    }

    public static void runQuietly(Runnable runnable) {
        try {
            runnable.run();
        }
        catch (Exception var2) {
            LOGGER.error("Failed to run runnable.", var2);
        }

    }

    public static ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    static {
        int max = Runtime.getRuntime().availableProcessors();
        scheduledExecutorService = Executors.newScheduledThreadPool(max * 2, new NamedThreadFactory("Autolimiter-Schedule"));
        executorService = new ThreadPoolExecutor(max * 2, max * 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), new NamedThreadFactory("Autolimiter-Executor"));
    }
}