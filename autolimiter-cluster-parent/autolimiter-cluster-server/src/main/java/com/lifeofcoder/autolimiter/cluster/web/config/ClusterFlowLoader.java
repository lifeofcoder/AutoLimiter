package com.lifeofcoder.autolimiter.cluster.web.config;

import com.alibaba.csp.sentinel.slots.block.flow.ClusterFlowConfig;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.lifeofcoder.autolimiter.cluster.flow.rule.ClusterFlowRule;
import com.lifeofcoder.autolimiter.cluster.flow.rule.ClusterFlowRuleManager;
import com.lifeofcoder.autolimiter.cluster.web.consts.FlowConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 集群配置加载
 *
 * @author xbc
 * @date 2021/6/30
 */
@Component
public class ClusterFlowLoader implements InitializingBean {
    private final static Logger LOGGER = LoggerFactory.getLogger(ClusterFlowLoader.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        List<ClusterFlowRule> clusterFlowRuleList = loadClusterFlowRule();

        //要判断版本号
        for (ClusterFlowRule clusterFlowRule : clusterFlowRuleList) {
            Long flowId = clusterFlowRule.getClusterConfig().getFlowId();
            FlowRule existFlowRule = ClusterFlowRuleManager.getFlowRuleById(flowId);
            if (null != existFlowRule) {
                if (!(existFlowRule instanceof ClusterFlowRule)) {
                    LOGGER.error("The format of message[" + flowId + "]  is invalid.");
                    continue;
                }

                ClusterFlowRule oldClusterFlowRule = (ClusterFlowRule) existFlowRule;

                //判断版本号,数据库中的版本号太旧
                if (oldClusterFlowRule.getVersion() >= clusterFlowRule.getVersion()) {
                    continue;
                }
                ClusterFlowRuleManager.loadRules(FlowConsts.DEFAULT_NAMESPACE, Arrays.asList(clusterFlowRule));
            }
            //不存在，直接添加
            else {
                ClusterFlowRuleManager.loadRules(FlowConsts.DEFAULT_NAMESPACE, Arrays.asList(clusterFlowRule));
            }
        }
        LOGGER.info("Succeeded to load " + clusterFlowRuleList.size() + " cluster flow rules.");
    }

    private List<ClusterFlowRule> loadClusterFlowRule() {
        ClusterFlowRule clusterFlowRule = new ClusterFlowRule();
        clusterFlowRule.setClusterCount(150D);
        clusterFlowRule.setCount(150D);
        clusterFlowRule.setLocalCount(100D);
        clusterFlowRule.setVersion(0);
        clusterFlowRule.setResource("hello");
        clusterFlowRule.setClusterMode(true);

        ClusterFlowConfig clusterFlowConfig = new ClusterFlowConfig();
        clusterFlowConfig.setFlowId(100001L);
        clusterFlowRule.setClusterConfig(clusterFlowConfig);

        List<ClusterFlowRule> clusterFlowRuleList = new ArrayList<>();
        clusterFlowRuleList.add(clusterFlowRule);
        return clusterFlowRuleList;
    }
}