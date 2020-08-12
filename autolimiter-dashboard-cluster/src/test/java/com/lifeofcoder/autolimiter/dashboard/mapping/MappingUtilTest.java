package com.lifeofcoder.autolimiter.dashboard.mapping;

import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.discovery.MachineInfo;
import org.junit.Assert;
import org.junit.Test;

public class MappingUtilTest {
    @Test
    public void getOrCreate() {
        FieldWrapper machineFieldWrapper = MappingUtil.getOrCreate(MachineInfo.class);
        MachineInfo machineInfo = new MachineInfo();
        machineFieldWrapper.set(machineInfo, "LAST_HEARTBEAT", 100);
        machineFieldWrapper.set(machineInfo, "APP", "bocai");
        machineFieldWrapper.set(machineInfo, "HEARTBEAT_VERSION", 12);
        machineFieldWrapper.set(machineInfo, "VERSION", "13L");
        Assert.assertEquals(100, machineInfo.getLastHeartbeat());
        Assert.assertEquals("bocai", machineInfo.getApp());
        Assert.assertEquals(12, machineInfo.getHeartbeatVersion());
        Assert.assertEquals("13L", machineInfo.getVersion());

    }
}