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
package com.lifeofcoder.autolimiter.cluster.server.processor;

import com.lifeofcoder.autolimiter.cluster.common.ClusterConstants;
import com.lifeofcoder.autolimiter.cluster.common.annotation.RequestType;
import com.lifeofcoder.autolimiter.cluster.common.request.ClusterRequest;
import com.lifeofcoder.autolimiter.cluster.common.request.data.DynamicFlowRequestData;
import com.lifeofcoder.autolimiter.cluster.common.response.ClusterResponse;
import com.lifeofcoder.autolimiter.cluster.common.response.data.DynamicFlowTokenResponseData;
import com.lifeofcoder.autolimiter.cluster.common.result.DynamicTokenResult;
import com.lifeofcoder.autolimiter.cluster.flow.DynamicTokenService;

/**
 * 动态流控处理类
 * @author xbc
 */
@RequestType(ClusterConstants.MSG_TYPE_DYNAMIC_FLOW)
public class DynamicFlowRequestProcessor implements RequestProcessor<DynamicFlowRequestData, DynamicFlowTokenResponseData> {
    private DynamicTokenService dynamicTokenService;

    public DynamicFlowRequestProcessor() {
        dynamicTokenService = new DynamicTokenService();
    }

    @Override
    public ClusterResponse<DynamicFlowTokenResponseData> processRequest(ClusterRequest<DynamicFlowRequestData> request) {
        DynamicTokenResult result = dynamicTokenService.requestDynamicToken(request.getData());
        return toResponse(result, request);
    }

    private ClusterResponse<DynamicFlowTokenResponseData> toResponse(DynamicTokenResult result, ClusterRequest request) {
        return new ClusterResponse<>(request.getId(), request.getType(), result.getStatus(), new DynamicFlowTokenResponseData().setCount(result.getCount()).setWaitInMs(result.getWaitInMs()));
    }
}
