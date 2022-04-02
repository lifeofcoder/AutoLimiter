package com.lifeofcoder.autolimiter.cluster.flow.client;

import java.io.Serializable;

/**
 * 客户端信息
 *
 * @author xbc
 * @date 2022/3/23
 */
public class ClientsInfo implements Serializable {
    private int clientNum;

    private int totalCount;

    private int deadClient;

    public ClientsInfo(int clientNum, int totalCount, int deadClient) {
        this.clientNum = clientNum;
        this.totalCount = totalCount;
        this.deadClient = deadClient;
    }

    public int getClientNum() {
        return clientNum;
    }

    public void setClientNum(int clientNum) {
        this.clientNum = clientNum;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getDeadClient() {
        return deadClient;
    }

    public void setDeadClient(int deadClient) {
        this.deadClient = deadClient;
    }
}
