package com.lifeofcoder.autolimiter.dashboard.model;

import java.io.Serializable;
import java.util.Date;

public interface BaseIgniteModel extends Serializable {
    String SCHEMA = "al";

    /**
     * 缓存存放的key
     */
    String key();

    default long longValue(Long val) {
        if (null == val) {
            return 0;
        }

        return val.longValue();
    }

    default long date2Long(Date val) {
        if (null == val) {
            return 0;
        }

        return val.getTime();
    }

    default Date long2Date(long val) {
        return new Date(val);
    }

    default int intValue(Integer val) {
        if (null == val) {
            return 0;
        }

        return val.intValue();
    }

    default double doubleValue(Double val) {
        if (null == val) {
            return 0;
        }

        return val.doubleValue();
    }
}
