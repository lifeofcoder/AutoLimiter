package com.lifeofcoder.autolimiter.dashboard.sentinel.customized;

import com.lifeofcoder.autolimiter.dashboard.ignite.dao.impl.IgniteMachineDao;
import com.lifeofcoder.autolimiter.dashboard.model.IgniteMachine;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.discovery.AppInfo;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.discovery.MachineInfo;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.discovery.SimpleMachineDiscovery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 覆盖默认的SimpleMachineDiscovery实现类
 * 通过Ignite分布式内存获取数据
 *
 * @author xbc
 * @date 2020/7/21
 */
@Component
@Primary
public class IgniteMachineDiscovery extends SimpleMachineDiscovery {

    @Autowired
    private IgniteMachineDao machineDao;

    @Override
    public long addMachine(MachineInfo machineInfo) {
        machineDao.addOrUpdate(new IgniteMachine(machineInfo));
        return 1;
    }

    @Override
    public boolean removeMachine(String app, String ip, int port) {
        return machineDao.delete(new IgniteMachine(app, ip, port));
    }

    @Override
    public List<String> getAppNames() {
        return machineDao.getAppNames();
    }

    @Override
    public AppInfo getDetailApp(String app) {
        List<IgniteMachine> machines = machineDao.getMachines(app);
        if (CollectionUtils.isEmpty(machines)) {
            return new AppInfo(app);
        }

        AppInfo appInfo = new AppInfo(app, machines.get(0).getMachineInfo().getAppType());
        for (IgniteMachine machine : machines) {
            appInfo.addMachine(machine.getMachineInfo());
        }

        return appInfo;
    }

    @Override
    public Set<AppInfo> getBriefApps() {
        List<IgniteMachine> igniteMachines = machineDao.listMachines();
        Set<AppInfo> appInfoSet = new HashSet<>(4);
        if (CollectionUtils.isEmpty(igniteMachines)) {
            return appInfoSet;
        }

        AppInfo tmpAppInfo = null;
        String currentApp = null;
        for (IgniteMachine igniteMachine : igniteMachines) {
            if (currentApp != igniteMachine.getMachineInfo().getApp()) {
                currentApp = igniteMachine.getMachineInfo().getApp();
                tmpAppInfo = new AppInfo(igniteMachine.getMachineInfo().getApp(), igniteMachine.getMachineInfo().getAppType());
                appInfoSet.add(tmpAppInfo);
            }
            tmpAppInfo.addMachine(igniteMachine.getMachineInfo());
        }

        return appInfoSet;
    }

    @Override
    public void removeApp(String app) {
        machineDao.deleteApp(app);
    }
}
