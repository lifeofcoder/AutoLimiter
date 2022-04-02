package com.lifeofcoder.autolimiter.cluster.web.metric;

import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricNode;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricNodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 打出日志
 * 已经接入控制台，可以去掉该功能了
 * @author xbc
 * @date 2021/6/11
 */
//@Service
public class MetricPrinter implements InitializingBean {
    private static Logger LOGGER = LoggerFactory.getLogger(MetricPrinter.class);

    ScheduledExecutorService scheduledExecutorService;

    @Override
    public void afterPropertiesSet() throws Exception {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            Map<String, List<ClusterMetricNode>> metricMap = ClusterMetricNodeGenerator.generateCurrentNodeMap("default");
            if (null == metricMap || metricMap.isEmpty()) {
                return;
            }
        }, 10, 1, TimeUnit.SECONDS);
    }
}
