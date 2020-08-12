package com.lifeofcoder.autolimiter.dashboard.ignite.dao.impl;

import com.lifeofcoder.autolimiter.dashboard.ignite.IgniteDao;
import com.lifeofcoder.autolimiter.dashboard.ignite.dao.MetricsDao;
import com.lifeofcoder.autolimiter.dashboard.model.BaseIgniteModel;
import com.lifeofcoder.autolimiter.dashboard.model.IgniteMetrics;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.datasource.entity.MetricEntity;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.QueryIndexType;
import org.apache.ignite.configuration.CacheConfiguration;
import org.springframework.stereotype.Repository;

import javax.cache.configuration.FactoryBuilder;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * IgniteMetricsDao
 *
 * @author xbc
 * @date 2020/7/22
 */
@Repository
public class IgniteMetricsDao implements MetricsDao {
    public static final String CACHE_NAME = "metrics";
    public static CacheConfiguration<Integer, IgniteMetrics> cacheCfg;
    private IgniteDao<IgniteMetrics> igniteDao;

    static {
        cacheCfg = new CacheConfiguration<>();
        cacheCfg.setName(CACHE_NAME);
        cacheCfg.setSqlSchema(BaseIgniteModel.SCHEMA);
        cacheCfg.setCacheMode(CacheMode.REPLICATED);
        //        cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        QueryEntity queryEntity = new QueryEntity();
        queryEntity.setKeyType(String.class.getName());
        queryEntity.setValueType(IgniteMetrics.class.getName());
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put(IgniteMetrics.COLUMN_APP, String.class.getName());
        fields.put(IgniteMetrics.COLUMN_RESOURCE, String.class.getName());
        fields.put(IgniteMetrics.COLUMN_GMT_CREATE, long.class.getName());
        fields.put(IgniteMetrics.COLUMN_GMT_MODIFIED, long.class.getName());
        fields.put(IgniteMetrics.COLUMN_TIMESTAMP, long.class.getName());
        fields.put(IgniteMetrics.COLUMN_PASS_QPS, Long.class.getName());
        fields.put(IgniteMetrics.COLUMN_SUCCESS_QPS, Long.class.getName());
        fields.put(IgniteMetrics.COLUMN_BLOCK_QPS, Long.class.getName());
        fields.put(IgniteMetrics.COLUMN_EXCEPTION_QPS, Long.class.getName());
        fields.put(IgniteMetrics.COLUMN_RT, double.class.getName());
        fields.put(IgniteMetrics.COLUMN_COUNT, int.class.getName());
        queryEntity.setFields(fields);
        queryEntity.setTableName(CACHE_NAME);
        QueryIndex queryIndex = new QueryIndex(Arrays.asList(IgniteMetrics.COLUMN_APP, IgniteMetrics.COLUMN_RESOURCE), QueryIndexType.SORTED);
        queryEntity.setIndexes(Arrays.asList(queryIndex));
        //缓存只停留5分钟
        cacheCfg.setExpiryPolicyFactory(FactoryBuilder.factoryOf(new CreatedExpiryPolicy(new Duration(TimeUnit.MINUTES, 5))));
        cacheCfg.setQueryEntities(Arrays.asList(queryEntity));
    }

    public IgniteMetricsDao(Ignite ignite) {
        igniteDao = new IgniteDao(ignite, cacheCfg);
    }

    @Override
    public void save(IgniteMetrics igniteMetrics) {
        igniteDao.addOrUpdate(igniteMetrics);
    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        StringBuilder sqlBuilder = new StringBuilder("select * from metrics where app='");
        sqlBuilder.append(app).append("' and resource='").append(resource).append("' and timestamp >= ");
        sqlBuilder.append(startTime).append(" and timestamp <= ").append(endTime);
        return igniteDao.query(sqlBuilder.toString(), MetricEntity.class);
    }

    @Override
    public List<MetricEntity> listOfApp(String app) {
        StringBuilder sqlBuilder = new StringBuilder("select * from metrics where app='").append(app).append("'");
        return igniteDao.query(sqlBuilder.toString(), MetricEntity.class);
    }
}