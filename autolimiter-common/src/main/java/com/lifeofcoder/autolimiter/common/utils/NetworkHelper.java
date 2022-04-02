package com.lifeofcoder.autolimiter.common.utils;

/**
 *
 *
 * @author xbc
 * @date 2022/4/2
 */
public class NetworkHelper {
    public NetworkHelper() {
    }

    public static long ipToLong(String strIp) throws RuntimeException {
        try {
            String[] ip = strIp.split("\\.");
            return (Long.parseLong(ip[0]) << 24) + (Long.parseLong(ip[1]) << 16) + (Long.parseLong(ip[2]) << 8) + Long.parseLong(ip[3]);
        }
        catch (Exception var2) {
            throw new RuntimeException("Failed to parse ip string to number.", var2);
        }
    }

    public static long ipToLong(String strIp, long defValue) {
        try {
            return ipToLong(strIp);
        }
        catch (RuntimeException var4) {
            return defValue;
        }
    }
}
