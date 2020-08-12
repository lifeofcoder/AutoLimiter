package com.lifeofcoder.autolimiter.dashboard.ignite.dao;

import com.lifeofcoder.autolimiter.dashboard.model.IgniteMetrics;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.datasource.entity.MetricEntity;

import java.util.List;

/**
 *
 *
 * @author xbc
 * @date 2020/7/22
 */
public interface MetricsDao {
    /**
     * 新增Metric
     * @param igniteMetrics 待新增的Metric
     */
    void save(IgniteMetrics igniteMetrics);

    /**
     * 根据条件查询Metric
     * @param app app名称
     * @param resource 资源名称
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 查询结果
     */
    List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime);

    /**
     * 查询APP下所有的Metric
     * @param app app名称
     * @return 查询结果
     */
    List<MetricEntity> listOfApp(String app);
}
