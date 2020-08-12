package com.lifeofcoder.autolimiter.client.heartbeat;

import com.alibaba.csp.sentinel.spi.SpiOrder;
import com.alibaba.csp.sentinel.transport.HeartbeatSender;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.transport.heartbeat.HeartbeatMessage;
import com.alibaba.csp.sentinel.transport.heartbeat.client.SimpleHttpClient;
import com.alibaba.csp.sentinel.transport.heartbeat.client.SimpleHttpRequest;
import com.alibaba.csp.sentinel.transport.heartbeat.client.SimpleHttpResponse;
import com.alibaba.csp.sentinel.util.function.Tuple2;
import com.lifeofcoder.autolimiter.client.ConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 轮询心跳发送(最高优先级)
 *
 * @author xbc
 * @date 2020/7/23
 */
@SpiOrder(SpiOrder.HIGHEST_PRECEDENCE)
public class PollHttpHeartbeatSender implements HeartbeatSender {
    /**
     * LOG
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(PollHttpHeartbeatSender.class);

    private static final int OK_STATUS = 200;

    private static final long DEFAULT_INTERVAL = 1000 * 10;

    private final HeartbeatMessage heartBeat = new HeartbeatMessage();
    private final SimpleHttpClient httpClient = new SimpleHttpClient();

    private int currentAddressIdx;

    private AtomicBoolean isHeartbeating = new AtomicBoolean(Boolean.FALSE);

    public PollHttpHeartbeatSender() {
        //生成一个随机值
        currentAddressIdx = new Random().nextInt();
    }

    @Override
    public boolean sendHeartbeat() throws Exception {
        if (TransportConfig.getRuntimePort() <= 0) {
            LOGGER.info("[SimpleHttpHeartbeatSender] Command server port not initialized, won't send heartbeat");
            return false;
        }

        if (isHeartbeating.get()) {
            return false;
        }

        if (!isHeartbeating.compareAndSet(Boolean.FALSE, Boolean.TRUE)) {
            return false;
        }

        LOGGER.debug("Try to send heartbeat to dashboard....");

        try {
            if (!sendHeartbeat0()) {
                return retry4SendHeartbeat();
            }

            return true;
        }
        finally {
            isHeartbeating.compareAndSet(Boolean.TRUE, Boolean.FALSE);
        }
    }

    private boolean sendHeartbeat0() throws IOException {
        Tuple2<String, Integer> addrInfo = getAvailableAddress();
        if (addrInfo == null) {
            LOGGER.error("There is no valid dabashboard addrsss.");
            return false;
        }

        //心跳发送失败，重试
        return doSendHeartbeat(addrInfo);
    }

    private boolean retry4SendHeartbeat() throws IOException {
        List<Tuple2<String, Integer>> addressList = ConfigListener.getAddressList();
        if (null == addressList) {
            return false;
        }

        //已经请求过一次了
        int size = addressList.size() - 1;
        int i = 0;
        while (i++ < size) {
            currentAddressIdx++;
            if (sendHeartbeat0()) {
                return true;
            }
        }

        LOGGER.warn("Failed to send heartbeat to dashboards.");
        return false;
    }

    private boolean doSendHeartbeat(Tuple2<String, Integer> addrInfo) throws IOException {
        InetSocketAddress addr = new InetSocketAddress(addrInfo.r1, addrInfo.r2);
        SimpleHttpRequest request = new SimpleHttpRequest(addr, TransportConfig.getHeartbeatApiPath());
        request.setParams(heartBeat.generateCurrentMessage());

        try {
            SimpleHttpResponse response = httpClient.post(request);
            if (response.getStatusCode() == OK_STATUS) {
                return true;
            }
            else if (clientErrorCode(response.getStatusCode()) || serverErrorCode(response.getStatusCode())) {
                LOGGER.warn("[SimpleHttpHeartbeatSender] Failed to send heartbeat to " + addr + ", http status code: " + response.getStatusCode());
            }
        }
        catch (Exception e) {
            LOGGER.warn("[SimpleHttpHeartbeatSender] Failed to send heartbeat to " + addr, e);
        }
        return false;
    }

    public long intervalMs() {
        return DEFAULT_INTERVAL;
    }

    private Tuple2<String, Integer> getAvailableAddress() {
        List<Tuple2<String, Integer>> addressList = ConfigListener.getAddressList();
        if (addressList == null || addressList.isEmpty()) {
            return null;
        }
        if (currentAddressIdx < 0) {
            currentAddressIdx = 0;
        }
        int index = currentAddressIdx % addressList.size();
        return addressList.get(index);
    }

    private boolean clientErrorCode(int code) {
        return code > 399 && code < 500;
    }

    private boolean serverErrorCode(int code) {
        return code > 499 && code < 600;
    }
}
