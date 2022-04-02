package com.lifeofcoder.autolimiter.cluster.web.controller;

import com.lifeofcoder.autolimiter.cluster.flow.client.ClientsInfo;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricStatistics;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.metric.ClusterMetric;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 *
 * @author xbc
 * @date 2022/3/23
 */
@RestController
@RequestMapping("/cluster")
public class ClusterController {

    @RequestMapping("/getClientsInfo")
    public ClientsInfo getClientsInfo(Long ruleId) {
        ClusterMetric metric = ClusterMetricStatistics.getMetricAndRecordHistory(ruleId);
        if (null == metric) {
            return null;
        }

        return metric.getClusterRuleClients().getClientsInfo();
    }
}
