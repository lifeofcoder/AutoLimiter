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
package com.lifeofcoder.autolimiter.cluster.client;

import com.alibaba.csp.sentinel.cluster.TokenResult;
import com.alibaba.csp.sentinel.cluster.TokenResultStatus;
import com.alibaba.csp.sentinel.cluster.TokenServerDescriptor;
import com.alibaba.fastjson.JSON;
import com.lifeofcoder.autolimiter.cluster.client.common.config.ClusterClientConfigManager;
import com.lifeofcoder.autolimiter.cluster.client.dynamic.DynamicClusterTokenClient;
import com.lifeofcoder.autolimiter.cluster.common.ClusterConstants;
import com.lifeofcoder.autolimiter.cluster.common.ClusterTransportClient;
import com.lifeofcoder.autolimiter.cluster.common.request.ClusterRequest;
import com.lifeofcoder.autolimiter.cluster.common.request.data.DynamicFlowRequestData;
import com.lifeofcoder.autolimiter.cluster.common.request.data.FlowRequestData;
import com.lifeofcoder.autolimiter.cluster.common.request.data.ParamFlowRequestData;
import com.lifeofcoder.autolimiter.cluster.common.response.ClusterResponse;
import com.lifeofcoder.autolimiter.cluster.common.response.data.DynamicFlowTokenResponseData;
import com.lifeofcoder.autolimiter.cluster.common.response.data.FlowTokenResponseData;
import com.lifeofcoder.autolimiter.cluster.common.result.DynamicTokenResult;
import com.lifeofcoder.autolimiter.common.consistent.IpPortHashNode;
import com.lifeofcoder.autolimiter.common.utils.NetworkHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ClusterTokenServiceClient
 */
public class ClusterTokenServiceClient extends IpPortHashNode implements DynamicClusterTokenClient {
    private static Logger LOGGER = LoggerFactory.getLogger(ClusterTokenServiceClient.class);

    private ClusterTransportClient transportClient;

    //这个字段表示的意思是，系统是否需要启动。而不是能不能启动
    private final AtomicBoolean shouldStart = new AtomicBoolean(false);

    private static long localIp;

    static {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            localIp = NetworkHelper.ipToLong(addr.getHostAddress());
        }
        catch (Exception e) {
            localIp = -1;
            LOGGER.error("Failed to get localIp.", e);
        }
    }

    public ClusterTokenServiceClient(String host, int port) {
        super(host, port);
        initNewConnection();
    }

    private void initNewConnection() {
        try {
            this.transportClient = new NettyTransportClient(getIp(), getPort());
            LOGGER.info("[CustomizedClusterTokenClient] A new client created: " + desc());
        }
        catch (Exception ex) {
            LOGGER.warn("[CustomizedClusterTokenClient] Failed to initialize new token client", ex);
        }
    }

    private void startClientIfScheduled() throws Exception {
        //shouldStart表示是否需要启动，只要系统已经启动了shouldStart=true.(不是表示是否是启动状态)
        //所以客户端变更的时候，因为shouldStart=true就会启动新的客户端了连接了
        if (shouldStart.get()) {
            if (transportClient != null) {
                transportClient.start();
            }
            else {
                LOGGER.warn("[CustomizedClusterTokenClient] Cannot start transport client: client not created");
            }
        }
    }

    private void stopClientIfStarted() throws Exception {
        if (shouldStart.compareAndSet(true, false)) {
            if (transportClient != null) {
                transportClient.stop();
            }
        }
    }

    @Override
    public void start() throws Exception {
        if (shouldStart.compareAndSet(false, true)) {
            startClientIfScheduled();
        }
    }

    @Override
    public void stop() throws Exception {
        stopClientIfStarted();
    }

    @Override
    public int getState() {
        if (transportClient == null) {
            return ClientConstants.CLIENT_STATUS_OFF;
        }
        return transportClient.isReady() ? ClientConstants.CLIENT_STATUS_STARTED : ClientConstants.CLIENT_STATUS_OFF;
    }

    @Override
    public TokenServerDescriptor currentServer() {
        return new TokenServerDescriptor(getIp(), getPort());
    }

    @Override
    public TokenResult requestToken(Long flowId, int acquireCount, boolean prioritized) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("The request of request token is [ruleId:" + flowId + ", acquireCount:" + acquireCount + ", prioritized:" + prioritized + "].");
        }
        TokenResult result = doRequestToken(flowId, acquireCount, prioritized);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("The response of request token is " + JSON.toJSONString(result));
        }
        return result;
    }

    private TokenResult doRequestToken(Long flowId, int acquireCount, boolean prioritized) {
        if (notValidRequest(flowId, acquireCount)) {
            return badRequest();
        }
        FlowRequestData data = new FlowRequestData().setCount(acquireCount).setFlowId(flowId).setPriority(prioritized);
        ClusterRequest<FlowRequestData> request = new ClusterRequest<>(ClusterConstants.MSG_TYPE_FLOW, data);
        try {
            TokenResult result = sendTokenRequest(request);
            return result;
        }
        catch (Exception ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to send token request to [" + desc() + "] with timeout[" + ClusterClientConfigManager.getRequestTimeout() + "].", ex);
            }
            return new TokenResult(TokenResultStatus.FAIL);
        }
    }

    @Override
    public TokenResult requestParamToken(Long flowId, int acquireCount, Collection<Object> params) {
        if (notValidRequest(flowId, acquireCount) || params == null || params.isEmpty()) {
            return badRequest();
        }
        ParamFlowRequestData data = new ParamFlowRequestData().setCount(acquireCount).setFlowId(flowId).setParams(params);
        ClusterRequest<ParamFlowRequestData> request = new ClusterRequest<>(ClusterConstants.MSG_TYPE_PARAM_FLOW, data);
        try {
            TokenResult result = sendTokenRequest(request);
            return result;
        }
        catch (Exception ex) {
            return new TokenResult(TokenResultStatus.FAIL);
        }
    }

    private TokenResult sendTokenRequest(ClusterRequest request) throws Exception {
        if (transportClient == null) {
            LOGGER.warn("[CustomizedClusterTokenClient] Client not created, please check your config for cluster client");
            return clientFail();
        }
        ClusterResponse response = transportClient.sendRequest(request);
        TokenResult result = new TokenResult(response.getStatus());
        if (response.getData() != null) {
            FlowTokenResponseData responseData = (FlowTokenResponseData) response.getData();
            result.setRemaining(responseData.getRemainingCount()).setWaitInMs(responseData.getWaitInMs());
        }
        return result;
    }

    @Override
    public DynamicTokenResult requestDynamicToken(long flowId, int maxCount, int lastCount) {
        if (transportClient == null) {
            LOGGER.warn("[CustomizedClusterTokenClient] Client not created, please check your config for cluster client");
            return new DynamicTokenResult(TokenResultStatus.FAIL);
        }

        try {
            DynamicFlowRequestData data = new DynamicFlowRequestData().setFlowId(flowId).setMaxCount(maxCount).setLastCount(lastCount).setIp(localIp);

            ClusterRequest<DynamicFlowRequestData> request = new ClusterRequest<>(ClusterConstants.MSG_TYPE_DYNAMIC_FLOW, data);

            ClusterResponse response = transportClient.sendRequest(request);
            DynamicTokenResult result = new DynamicTokenResult(response.getStatus());
            if (response.getData() != null) {
                DynamicFlowTokenResponseData responseData = (DynamicFlowTokenResponseData) response.getData();
                result.setCount(responseData.getCount()).setWaitInMs(responseData.getWaitInMs());
            }
            return result;
        }
        catch (Exception e) {
            return new DynamicTokenResult(TokenResultStatus.FAIL);
        }
    }

    private boolean notValidRequest(Long id, int count) {
        return id == null || id <= 0 || count <= 0;
    }

    private TokenResult badRequest() {
        return new TokenResult(TokenResultStatus.BAD_REQUEST);
    }

    private TokenResult clientFail() {
        return new TokenResult(TokenResultStatus.FAIL);
    }
}
