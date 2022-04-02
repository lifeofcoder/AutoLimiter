package com.lifeofcoder.autolimiter.common.config;

import java.io.Serializable;
import java.util.Objects;

/**
 * 服务端配置
 *
 * @author xbc
 * @date 2021/7/21
 */
public class ClusterServerConfigItem implements Serializable, HashNode {
    private String ip;
    private Integer port;
    /**
     * 可用率，比如0.9就表示节点可用率低于90%则表示不可用需要降级
     */
    private Double availableRatio;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Double getAvailableRatio() {
        return availableRatio;
    }

    public void setAvailableRatio(Double availableRatio) {
        this.availableRatio = availableRatio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ClusterServerConfigItem that = (ClusterServerConfigItem) o;
        return Objects.equals(ip, that.ip) && Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    @Override
    public String key() {
        return ip + ":" + port;
    }

    @Override
    public String toString() {
        return key();
    }
}
