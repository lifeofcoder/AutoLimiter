package com.lifeofcoder.autolimiter.cluster.server.command.handler;

import com.alibaba.csp.sentinel.Constants;
import com.alibaba.csp.sentinel.command.CommandHandler;
import com.alibaba.csp.sentinel.command.CommandRequest;
import com.alibaba.csp.sentinel.command.CommandResponse;
import com.alibaba.csp.sentinel.command.annotation.CommandMapping;
import com.alibaba.csp.sentinel.node.metric.MetricNode;
import com.alibaba.csp.sentinel.node.metric.MetricSearcher;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.csp.sentinel.util.TimeUtil;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricNode;
import com.lifeofcoder.autolimiter.cluster.flow.statistic.ClusterMetricNodeGenerator;
import com.lifeofcoder.autolimiter.cluster.web.consts.FlowConsts;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CommandMapping(name = "metric", desc = "get and aggregate metrics, accept param: " + "startTime={startTime}&endTime={endTime}&maxLines={maxLines}&identify={resourceName}")
public class SendClusterMetricCommandHandler implements CommandHandler<String> {

    private volatile MetricSearcher searcher;

    @Override
    public CommandResponse<String> handle(CommandRequest request) {
        String startTimeStr = request.getParam("startTime");
        String endTimeStr = request.getParam("endTime");
        String maxLinesStr = request.getParam("maxLines");
        String identity = request.getParam("identity");
        long startTime = -1;
        long endTime = -1;
        if (StringUtil.isNotBlank(startTimeStr)) {
            startTime = Long.parseLong(startTimeStr);
        }
        else {
            return CommandResponse.ofSuccess("");
        }

        if (StringUtil.isNotBlank(endTimeStr)) {
            endTime = Long.parseLong(endTimeStr);
        }
        else {
            return CommandResponse.ofSuccess("");
        }

        // Note: not thread-safe.
        //key : resourceName
        //时间区间[startTime, endTime]
        Map<String, List<ClusterMetricNode>> clusterMetricMap = ClusterMetricNodeGenerator.generateCurrentNodeMap(FlowConsts.DEFAULT_NAMESPACE, startTime, endTime);
        if (CollectionUtils.isEmpty(clusterMetricMap)) {
            return CommandResponse.ofSuccess(null);
        }

        //转换为MetricNode
        List<MetricNode> metricNodeList = new ArrayList<>(clusterMetricMap.size());
        MetricNode tmpMetricNode;
        for (Map.Entry<String, List<ClusterMetricNode>> entry : clusterMetricMap.entrySet()) {
            if (CollectionUtils.isEmpty(entry.getValue())) {
                continue;
            }

            for (ClusterMetricNode clusterMetricNode : entry.getValue()) {
                tmpMetricNode = new MetricNode();
                tmpMetricNode.setPassQps(Double.valueOf(clusterMetricNode.getPassQps()).longValue());
                tmpMetricNode.setResource(clusterMetricNode.getResourceName());
                tmpMetricNode.setTimestamp(clusterMetricNode.getTimestamp());
                tmpMetricNode.setBlockQps(Double.valueOf(clusterMetricNode.getBlockQps()).longValue());
                tmpMetricNode.setRt(clusterMetricNode.getRt());
                metricNodeList.add(tmpMetricNode);
            }
        }

        addCpuUsageAndLoad(metricNodeList);

        StringBuilder sb = new StringBuilder();
        for (MetricNode node : metricNodeList) {
            sb.append(node.toThinString()).append("\n");
        }
        return CommandResponse.ofSuccess(sb.toString());
    }

    /**
     * add current cpu usage and load to the metric list.
     *
     * @param list metric list, should not be null
     */
    private void addCpuUsageAndLoad(List<MetricNode> list) {
        long time = TimeUtil.currentTimeMillis() / 1000 * 1000;
        double load = SystemRuleManager.getCurrentSystemAvgLoad();
        double usage = SystemRuleManager.getCurrentCpuUsage();
        if (load > 0) {
            MetricNode loadNode = toNode(load, time, Constants.SYSTEM_LOAD_RESOURCE_NAME);
            list.add(loadNode);
        }
        if (usage > 0) {
            MetricNode usageNode = toNode(usage, time, Constants.CPU_USAGE_RESOURCE_NAME);
            list.add(usageNode);
        }
    }

    /**
     * transfer the value to a MetricNode, the value will multiply 10000 then truncate
     * to long value, and as the {@link MetricNode#passQps}.
     * <p>
     * This is an eclectic scheme before we have a standard metric format.
     * </p>
     *
     * @param value    value to save.
     * @param ts       timestamp
     * @param resource resource name.
     * @return a MetricNode represents the value.
     */
    private MetricNode toNode(double value, long ts, String resource) {
        MetricNode node = new MetricNode();
        node.setPassQps((long) (value * 10000));
        node.setTimestamp(ts);
        node.setResource(resource);
        return node;
    }
}
