package com.lifeofcoder.autolimiter.client;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.csp.sentinel.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置监听，用于配置控制台地址信息
 *
 * @author xbc
 * @date 2020/4/21
 */
public class ConfigListener {
    /**
     * 控制台地址
     */
    private static volatile List<Tuple2<String, Integer>> addressList;

    public static List<Tuple2<String, Integer>> getAddressList() {
        return addressList;
    }

    /**
     * 构造函数
     *
     * @param dashboardAdds 控制台地址
     */
    public ConfigListener(String dashboardAdds) {
        if (StringUtil.isBlank(dashboardAdds)) {
            throw new AutoLimiterException("The addresses of dashboard must be set.");
        }

        addressList = parseAddress(dashboardAdds);
    }

    private List<Tuple2<String, Integer>> parseAddress(String config) {
        List<Tuple2<String, Integer>> list = new ArrayList<Tuple2<String, Integer>>();
        if (StringUtil.isBlank(config)) {
            return list;
        }

        int pos = -1;
        int cur = 0;
        while (true) {
            pos = config.indexOf(',', cur);
            if (cur < config.length() - 1 && pos < 0) {
                // for single segment, pos move to the end
                pos = config.length();
            }
            if (pos < 0) {
                break;
            }
            if (pos <= cur) {
                cur++;
                continue;
            }
            // parsing
            String ipPortStr = config.substring(cur, pos);
            cur = pos + 1;
            if (StringUtil.isBlank(ipPortStr)) {
                continue;
            }
            ipPortStr = ipPortStr.trim();
            if (ipPortStr.startsWith("http://")) {
                ipPortStr = ipPortStr.substring(7);
            }
            int index = ipPortStr.indexOf(":");
            int port = 80;
            if (index == 0) {
                // skip
                continue;
            }
            String host = ipPortStr;
            if (index >= 0) {
                try {
                    port = Integer.parseInt(ipPortStr.substring(index + 1));
                    if (port <= 1 || port >= 65535) {
                        throw new RuntimeException("Port number [" + port + "] over range");
                    }
                }
                catch (Exception e) {
                    RecordLog.warn("Parse port of dashboard server failed: " + ipPortStr, e);
                    // skip
                    continue;
                }
                host = ipPortStr.substring(0, index);
            }
            list.add(Tuple2.of(host, port));
        }
        return list;
    }
}
