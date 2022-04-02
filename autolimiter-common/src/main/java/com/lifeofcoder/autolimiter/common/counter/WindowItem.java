package com.lifeofcoder.autolimiter.common.counter;

import java.util.concurrent.atomic.LongAdder;

/**
 * 滑动窗口项
 *
 * @author xbc
 * @date 2022/4/2
 */
public class WindowItem {
    private LongAdder counter;
    private volatile long startTimeInMs;
    private int timeIntervalInMs;

    public WindowItem(long startTimeInMs, int timeIntervalInMs) {
        this.startTimeInMs = startTimeInMs;
        this.timeIntervalInMs = timeIntervalInMs;
        this.counter = new LongAdder();
    }

    public void reset(long newStartTimeInMs) {
        this.counter.reset();
        this.startTimeInMs = newStartTimeInMs;
    }

    public boolean isValid(long timeInMs, int totalTimeInMs) {
        return this.startTimeInMs <= timeInMs && this.startTimeInMs + (long) totalTimeInMs > timeInMs;
    }

    public long getCount() {
        return this.counter.sum();
    }

    public void add(long delta) {
        this.counter.add(delta);
    }

    public long getStartTimeInMs() {
        return this.startTimeInMs;
    }

    public static boolean isValid(WindowItem windowItem, long timeInMs, int totalTimeInMs) {
        return null != windowItem && windowItem.isValid(timeInMs, totalTimeInMs);
    }
}
