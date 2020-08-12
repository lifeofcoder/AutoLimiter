package com.lifeofcoder.autolimiter.dashboard.sentinel.customized;

import com.lifeofcoder.autolimiter.dashboard.ignite.dao.impl.IgniteMetricsDao;
import com.lifeofcoder.autolimiter.dashboard.model.IgniteMetrics;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.datasource.entity.MetricEntity;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.repository.metric.MetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于Ignite的Metrics存储
 *
 * @author xbc
 * @date 2020/7/22
 */
@Component
@Primary
public class IgniteMetricsRepository implements MetricsRepository<MetricEntity> {
    @Autowired
    private IgniteMetricsDao metricsDao;

    @Override
    public void save(MetricEntity metric) {
        metricsDao.save(new IgniteMetrics(metric));
    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        metrics.forEach(this::save);
    }

    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        return metricsDao.queryByAppAndResourceBetween(app, resource, startTime, endTime);
    }

    //根据请求量倒排序
    @Override
    public List<String> listResourcesOfApp(String app) {
        List<MetricEntity> metricEntities = metricsDao.listOfApp(app);
        final long minTimeMs = System.currentTimeMillis() - 1000 * 60;
        //resource -> MetricEntity
        Map<String, MetricEntity> resourceCount = new ConcurrentHashMap<>(32);

        for (MetricEntity metricEntity : metricEntities) {
            if (metricEntity.getTimestamp().getTime() < minTimeMs) {
                continue;
            }
            if (resourceCount.containsKey(metricEntity.getResource())) {
                MetricEntity oldEntity = resourceCount.get(metricEntity.getResource());
                oldEntity.addPassQps(metricEntity.getPassQps());
                oldEntity.addRtAndSuccessQps(metricEntity.getRt(), metricEntity.getSuccessQps());
                oldEntity.addBlockQps(metricEntity.getBlockQps());
                oldEntity.addExceptionQps(metricEntity.getExceptionQps());
                oldEntity.addCount(1);
            }
            else {
                resourceCount.put(metricEntity.getResource(), MetricEntity.copyOf(metricEntity));
            }
        }
        // Order by last minute b_qps DESC.
        return resourceCount.entrySet().stream().sorted((o1, o2) -> {
            MetricEntity e1 = o1.getValue();
            MetricEntity e2 = o2.getValue();
            int t = e2.getBlockQps().compareTo(e1.getBlockQps());
            if (t != 0) {
                return t;
            }
            return e2.getPassQps().compareTo(e1.getPassQps());
        }).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}
