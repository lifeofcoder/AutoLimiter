package com.lifeofcoder.autolimiter.dashboard.ignite.dao;

import com.lifeofcoder.autolimiter.dashboard.ignite.IgniteDao;
import org.apache.ignite.cache.query.SqlFieldsQuery;

import java.util.List;

/**
 * BaseIgniteDao
 *
 * @author xbc
 * @date 2020/7/21
 */
public abstract class BaseIgniteDao<T> {
    protected IgniteDao igniteDao;

    private Class<T> modeClass;

    public BaseIgniteDao(IgniteDao igniteDao) {
        this.igniteDao = igniteDao;
    }

    public List<T> queryObject(SqlFieldsQuery sqlFieldsQuery) {
        T t = null;
        try {
            t = modeClass.newInstance();
        }
        catch (InstantiationException e) {
        }
        catch (IllegalAccessException e) {
        }

        return null;
    }
}
