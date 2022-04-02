package com.lifeofcoder.autolimiter.common.counter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 滑动窗口
 *
 * @author xbc
 * @date 2022/4/2
 */
public class SlidingWindow {
    private static final long DELTA_ONE = 1L;
    private final ReentrantLock resetLock;
    private final AtomicReferenceArray<WindowItem> atomicWindowItems;
    private final int windowCount;
    private final int totalTimeInMs;
    private final int windowIntervalInMs;

    public SlidingWindow(int windowCount) {
        this(windowCount, Duration.ofSeconds(1L));
    }

    public SlidingWindow(int windowCount, Duration windowDuration) {
        this.resetLock = new ReentrantLock();
        long totalTimeMs = windowDuration.toMillis();
        if (windowCount > 0 && totalTimeMs > 0L) {
            this.windowCount = windowCount;
            this.totalTimeInMs = (int) totalTimeMs;
            this.windowIntervalInMs = this.totalTimeInMs / windowCount;
            this.atomicWindowItems = new AtomicReferenceArray(windowCount);
        }
        else {
            throw new IllegalArgumentException("WindowCount or timeInSec is invalid.");
        }
    }

    public long count(long currentTimeMillis) {
        long totalCount = 0L;

        for (int i = 0; i < this.windowCount; ++i) {
            WindowItem tempWindowItem = (WindowItem) this.atomicWindowItems.get(i);
            if (WindowItem.isValid(tempWindowItem, currentTimeMillis, this.totalTimeInMs)) {
                totalCount += tempWindowItem.getCount();
            }
        }

        return totalCount;
    }

    public long count() {
        long now = System.currentTimeMillis();
        return this.count(now);
    }

    public void add() {
        WindowItem windowItem = this.currentWindow();
        windowItem.add(1L);
    }

    public WindowItem getWindow(long currentTimeMillis) {
        int windowIdx = this.windowIndex(currentTimeMillis);
        WindowItem windowItem = (WindowItem) this.atomicWindowItems.get(windowIdx);
        if (null == windowItem) {
            windowItem = this.createWindowItem(windowIdx, currentTimeMillis);
        }
        else if (!windowItem.isValid(currentTimeMillis, this.totalTimeInMs)) {
            this.resetWindow(windowItem, currentTimeMillis, this.calcStartTime4WindowItem(currentTimeMillis));
        }

        return windowItem;
    }

    private void resetWindow(WindowItem windowItem, long currentTimeMillis, long startTimeInMs) {
        while (!windowItem.isValid(currentTimeMillis, this.totalTimeInMs)) {
            if (this.resetLock.tryLock()) {
                try {
                    windowItem.reset(startTimeInMs);
                }
                finally {
                    this.resetLock.unlock();
                }

                return;
            }

            Thread.yield();
        }

    }

    private WindowItem createWindowItem(int windowIdx, long currentTimeMillis) {
        long startTime = this.calcStartTime4WindowItem(currentTimeMillis);
        WindowItem newWindowItem = new WindowItem(startTime, this.windowIntervalInMs);

        while (!this.atomicWindowItems.compareAndSet(windowIdx, (WindowItem) null, newWindowItem)) {
            Thread.yield();
            WindowItem windowItem = (WindowItem) this.atomicWindowItems.get(windowIdx);
            if (null != windowItem) {
                return windowItem;
            }
        }

        return newWindowItem;
    }

    private long calcStartTime4WindowItem(long currentTimeMillis) {
        return currentTimeMillis - currentTimeMillis % (long) this.windowIntervalInMs;
    }

    private int windowIndex(long currentTimeMillis) {
        return (int) (currentTimeMillis / (long) this.windowIntervalInMs % (long) this.windowCount);
    }

    public WindowItem currentWindow() {
        long now = System.currentTimeMillis();
        return this.getWindow(now);
    }
}
