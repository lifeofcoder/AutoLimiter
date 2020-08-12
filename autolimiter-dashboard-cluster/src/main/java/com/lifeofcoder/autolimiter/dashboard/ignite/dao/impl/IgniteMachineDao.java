package com.lifeofcoder.autolimiter.dashboard.ignite.dao.impl;

import com.lifeofcoder.autolimiter.dashboard.ignite.IgniteDao;
import com.lifeofcoder.autolimiter.dashboard.ignite.dao.MachineDao;
import com.lifeofcoder.autolimiter.dashboard.model.BaseIgniteModel;
import com.lifeofcoder.autolimiter.dashboard.model.IgniteMachine;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.discovery.MachineInfo;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cache.QueryIndex;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 机器查询DAO实现类
 *
 * @author xbc
 * @date 2020/7/17
 */
@Repository
public class IgniteMachineDao implements MachineDao {
    public static final String CACHE_NAME = "machine";
    public static CacheConfiguration<Integer, IgniteMachine> cacheCfg;
    private IgniteDao<IgniteMachine> igniteDao;

    static {
        cacheCfg = new CacheConfiguration<>();
        cacheCfg.setName(CACHE_NAME);
        cacheCfg.setSqlSchema(BaseIgniteModel.SCHEMA);
        cacheCfg.setCacheMode(CacheMode.REPLICATED);
        //        cacheCfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        QueryEntity queryEntity = new QueryEntity();
        queryEntity.setKeyType(String.class.getName());
        queryEntity.setValueType(IgniteMachine.class.getName());
        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        fields.put(IgniteMachine.COLUMN_ID, String.class.getName());
        fields.put(IgniteMachine.COLUMN_APP, String.class.getName());
        fields.put(IgniteMachine.COLUMN_APP_TYPE, Integer.class.getName());
        fields.put(IgniteMachine.COLUMN_IP, String.class.getName());
        fields.put(IgniteMachine.COLUMN_PORT, Integer.class.getName());
        fields.put(IgniteMachine.COLUMN_VERSION, String.class.getName());
        fields.put(IgniteMachine.COLUMN_LAST_HEARTBEAT, Long.class.getName());
        fields.put(IgniteMachine.COLUMN_HEARTBEAT_VERSION, Long.class.getName());
        fields.put(IgniteMachine.COLUMN_HOSTNAME, String.class.getName());
        queryEntity.setFields(fields);
        queryEntity.setTableName(CACHE_NAME);
        //创建两个索引
        queryEntity.setIndexes(Arrays.asList(new QueryIndex(IgniteMachine.COLUMN_ID), new QueryIndex(IgniteMachine.COLUMN_APP)));
        cacheCfg.setQueryEntities(Arrays.asList(queryEntity));
    }

    public IgniteMachineDao(Ignite ignite) {
        igniteDao = new IgniteDao(ignite, cacheCfg);
    }

    @Override
    public void addOrUpdate(IgniteMachine machine) {
        igniteDao.addOrUpdate(machine);
    }

    @Override
    public boolean delete(IgniteMachine machine) {
        return igniteDao.deleteByKey(machine.key());
    }

    @Override
    public List<String> getAppNames() {
        List<Map<String, Object>> appNames = igniteDao.query(new SqlFieldsQuery("select distinct app from machine"));
        List<String> appNameList = appNames.stream().map(f -> f.get("APP").toString()).collect(Collectors.toList());
        return appNameList;
    }

    @Override
    public List<IgniteMachine> getMachines(String app) {
        String sql = "select * from machine where app ='" + app + "'";
        List<MachineInfo> machineInfoList = igniteDao.query(sql, MachineInfo.class);
        return machineInfoList.stream().map(m -> new IgniteMachine(m)).collect(Collectors.toList());
    }

    @Override
    public void deleteApp(String app) {
        String sql = "delete from machine where app ='" + app + "'";
        igniteDao.query(new SqlFieldsQuery(sql));
    }

    @Override
    public List<IgniteMachine> listMachines() {
        String sql = "select * from machine order by app";
        List<MachineInfo> machineInfoList = igniteDao.query(sql, MachineInfo.class);
        return machineInfoList.stream().map(m -> new IgniteMachine(m)).collect(Collectors.toList());
    }
}
