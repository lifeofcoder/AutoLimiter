/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lifeofcoder.autolimiter.cluster.flow;

import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.lifeofcoder.autolimiter.cluster.common.request.data.DynamicFlowRequestData;
import com.lifeofcoder.autolimiter.cluster.common.result.DynamicTokenResult;
import com.lifeofcoder.autolimiter.cluster.flow.rule.ClusterFlowRuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动态Token服务
 * @author xbc
 */
public class DynamicTokenService {
    private static Logger LOGGER = LoggerFactory.getLogger(DynamicTokenService.class);

    public DynamicTokenResult requestDynamicToken(DynamicFlowRequestData requestData) {
        DynamicTokenResult result = doRequestToken(requestData);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The requestDynamicToken for [" + requestData.getFlowId() +  "] result is " + result.getCount());
        }
        return result;
    }

    private DynamicTokenResult doRequestToken(DynamicFlowRequestData requestData) {
        if (notValidRequest(requestData.getFlowId())) {
            return badRequest();
        }
        // The rule should be valid.
        FlowRule rule = ClusterFlowRuleManager.getFlowRuleById(requestData.getFlowId());
        if (rule == null) {
            return new DynamicTokenResult(TokenResultStatus.NO_RULE_EXISTS);
        }

        return DynamicClusterFlowChecker.acquireClusterToken(rule, requestData);
    }

    private boolean notValidRequest(Long id) {
        return id == null || id <= 0;
    }

    private DynamicTokenResult badRequest() {
        return new DynamicTokenResult(TokenResultStatus.BAD_REQUEST);
    }
}
