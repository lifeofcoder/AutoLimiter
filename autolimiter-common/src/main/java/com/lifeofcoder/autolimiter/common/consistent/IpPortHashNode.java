package com.lifeofcoder.autolimiter.common.consistent;

import com.lifeofcoder.autolimiter.common.config.HashNode;
import com.lifeofcoder.autolimiter.common.utils.NetworkHelper;

import java.util.Objects;

/**
 *
 *
 * @author xbc
 * @date 2022/4/2
 */
public class IpPortHashNode implements HashNode {
    private String ip;
    private int port;

    public IpPortHashNode(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String key() {
        try {
            return String.valueOf(NetworkHelper.ipToLong(this.ip) + (long) this.port);
        }
        catch (Exception var2) {
            return this.port + ":" + this.ip;
        }
    }

    public String desc() {
        return this.ip + ":" + this.port;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        else if (o != null && this.getClass() == o.getClass()) {
            IpPortHashNode that = (IpPortHashNode) o;
            return this.port == that.port && Objects.equals(this.ip, that.ip);
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[] {this.ip, this.port});
    }
}
