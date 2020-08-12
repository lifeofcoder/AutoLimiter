package com.lifeofcoder.autolimiter.client;

/**
 * 自动限流项目异常
 *
 * @author xbc
 * @date 2020/4/23
 */
public class AutoLimiterException extends RuntimeException {
    public AutoLimiterException() {
    }

    public AutoLimiterException(String message) {
        super(message);
    }

    public AutoLimiterException(String message, Throwable cause) {
        super(message, cause);
    }

    public AutoLimiterException(Throwable cause) {
        super(cause);
    }

    public AutoLimiterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
