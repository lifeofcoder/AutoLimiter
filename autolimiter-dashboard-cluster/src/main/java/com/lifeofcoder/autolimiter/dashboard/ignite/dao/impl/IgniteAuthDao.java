package com.lifeofcoder.autolimiter.dashboard.ignite.dao.impl;

import com.lifeofcoder.autolimiter.dashboard.ignite.IgniteDao;
import com.lifeofcoder.autolimiter.dashboard.ignite.dao.AuthDao;
import com.lifeofcoder.autolimiter.dashboard.model.BaseIgniteModel;
import com.lifeofcoder.autolimiter.dashboard.model.IgniteAuth;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.configuration.CacheConfiguration;
import org.springframework.stereotype.Repository;

import javax.cache.configuration.FactoryBuilder;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

/**
 * IgniteAuthDao
 *
 * @author xbc
 * @date 2020/7/29
 */
@Repository
public class IgniteAuthDao implements AuthDao {
    public static final String CACHE_NAME = "authority";
    public static CacheConfiguration<String, IgniteAuth> cacheCfg;
    private IgniteDao<IgniteAuth> igniteDao;

    static {
        cacheCfg = new CacheConfiguration<>();
        cacheCfg.setName(CACHE_NAME);
        cacheCfg.setSqlSchema(BaseIgniteModel.SCHEMA);
        cacheCfg.setCacheMode(CacheMode.REPLICATED);
        //        cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        QueryEntity queryEntity = new QueryEntity();
        queryEntity.setKeyType(String.class.getName());
        queryEntity.setValueType(IgniteAuth.class.getName());
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put(IgniteAuth.COLUMN_ID, String.class.getName());
        fields.put(IgniteAuth.COLUMN_NAME, String.class.getName());
        queryEntity.setFields(fields);
        queryEntity.setTableName(CACHE_NAME);
        //创建两个索引
        queryEntity.setIndexes(Arrays.asList(new QueryIndex(IgniteAuth.COLUMN_ID)));
        //缓存只停留10分钟
        cacheCfg.setExpiryPolicyFactory(FactoryBuilder.factoryOf(new CreatedExpiryPolicy(new Duration(TimeUnit.MINUTES, 10))));
        cacheCfg.setQueryEntities(Arrays.asList(queryEntity));
    }

    public IgniteAuthDao(Ignite ignite) {
        igniteDao = new IgniteDao(ignite, cacheCfg);
    }

    @Override
    public void addUser(IgniteAuth authUser) {
        igniteDao.addOrUpdate(authUser);
    }

    @Override
    public IgniteAuth getUser(String sessionId) {
        return igniteDao.getByKey(sessionId);
    }
}
