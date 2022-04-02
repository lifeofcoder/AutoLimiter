package com.lifeofcoder.autolimiter.common.utils;

import io.netty.util.internal.SystemPropertyUtil;

import java.util.Locale;

/**
 * 操作系统工具
 *
 * @author xbc
 * @date 2022/4/2
 */
public class OsHelper {
    public OsHelper() {
    }

    public static OsHelper.OsType getOsType() {
        String name = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
        if (name.startsWith("linux")) {
            return OsHelper.OsType.LINUX;
        }
        else if (name.startsWith("mac")) {
            return OsHelper.OsType.MAC;
        }
        else {
            return name.startsWith("windows") ? OsHelper.OsType.WIN : OsHelper.OsType.OTHER;
        }
    }

    public static enum OsType {
        MAC, WIN, LINUX, OTHER;

        private OsType() {
        }
    }
}
