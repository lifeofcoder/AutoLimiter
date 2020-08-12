package com.lifeofcoder.autolimiter.dashboard.ignite.dao;

import com.lifeofcoder.autolimiter.dashboard.model.IgniteMachine;

import java.util.List;

/**
 * 机器查询DAO
 *
 * @author xbc
 * @date 2020/7/17
 */
public interface MachineDao {
    void addOrUpdate(IgniteMachine machine);

    boolean delete(IgniteMachine machine);

    List<String> getAppNames();

    List<IgniteMachine> getMachines(String app);

    void deleteApp(String app);

    List<IgniteMachine> listMachines();
}
