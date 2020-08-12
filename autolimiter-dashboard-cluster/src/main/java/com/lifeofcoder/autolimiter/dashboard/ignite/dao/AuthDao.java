package com.lifeofcoder.autolimiter.dashboard.ignite.dao;

import com.lifeofcoder.autolimiter.dashboard.model.IgniteAuth;

/**
 * Auth Dao
 *
 * @author xbc
 * @date 2020/7/29
 */
public interface AuthDao {
    void addUser(IgniteAuth authUser);

    IgniteAuth getUser(String sessionId);
}
