package com.lifeofcoder.autolimiter.dashboard.sentinel.customized.rule;

import com.alibaba.csp.sentinel.util.AssertUtil;
import com.lifeofcoder.autolimiter.dashboard.model.BaseIgniteModel;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.datasource.entity.rule.RuleEntity;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.discovery.MachineInfo;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.repository.rule.InMemoryRuleRepositoryAdapter;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ignite的规则存储
 * @param <T>
 */
public abstract class IgniteRuleRepositoryAdapter<T extends RuleEntity> extends InMemoryRuleRepositoryAdapter<T> implements InitializingBean {

    private IgniteCache cache;

    private IgniteAtomicLong igniteAtomicLong;

    @Autowired
    private Ignite ignite;

    @Override
    public T save(T entity) {
        if (entity.getId() == null) {
            entity.setId(nextId());
        }
        T processedEntity = preProcess(entity);
        if (processedEntity != null) {
            saveAllRule(processedEntity);
            saveMachineRule(processedEntity);
            saveAppRule(processedEntity);
        }

        return processedEntity;
    }

    private void saveAllRule(T entity) {
        cache.put(key4AllRules(entity.getId()), entity);
    }

    private String key4AllRules(Long id) {
        return cacheName() + ".allRules." + id;
    }

    private void saveMachineRule(T entity) {
        String key = key4MachineRules(entity);
        Map<Long, T> rulesMap = (Map<Long, T>) cache.get(key);
        if (null == rulesMap) {
            rulesMap = new ConcurrentHashMap<>(16);
        }
        rulesMap.put(entity.getId(), entity);
        cache.put(key, rulesMap);
    }

    private String key4MachineRules(T entity) {
        return key4MachineRules(MachineInfo.of(entity.getApp(), entity.getIp(), entity.getPort()));
    }

    private String key4MachineRules(MachineInfo machineInfo) {
        StringBuilder keyBuilder = new StringBuilder(cacheName());
        keyBuilder.append(".machineRules.").append(machineInfo.getApp()).append(machineInfo.getIp()).append(machineInfo.getPort());
        return keyBuilder.toString();
    }

    private void saveAppRule(T entity) {
        String key = key4AppRules(entity.getApp());
        Map<Long, T> rulesMap = (Map<Long, T>) cache.get(key);
        if (null == rulesMap) {
            rulesMap = new ConcurrentHashMap<>(16);
        }
        rulesMap.put(entity.getId(), entity);
        cache.put(key, rulesMap);
    }

    private String key4AppRules(String app) {
        return cacheName() + ".appRules." + app;
    }

    private T deleteAllRule(Long id) {
        return (T) cache.getAndRemove(key4AllRules(id));
    }

    private void deleteMachineRule(T entity) {
        String key = key4MachineRules(entity);
        Map<Long, T> rulesMap = (Map<Long, T>) cache.get(key);
        if (null == rulesMap || !rulesMap.containsKey(entity.getId())) {
            return;
        }
        rulesMap.remove(entity.getId());
        cache.put(key, rulesMap);
    }

    private void deleteAppRule(T entity) {
        String key = key4AppRules(entity.getApp());
        Map<Long, T> rulesMap = (Map<Long, T>) cache.get(key);
        if (null == rulesMap || !rulesMap.containsKey(entity.getId())) {
            return;
        }
        rulesMap.remove(entity.getId());
        cache.put(key, rulesMap);
    }

    private T findAllRule(Long id) {
        return (T) cache.get(key4AllRules(id));
    }

    private Map<Long, T> findMachineRule(MachineInfo machineInfo) {
        return (Map<Long, T>) cache.get(key4MachineRules(machineInfo));
    }

    private Map<Long, T> findAppRule(String app) {
        String key = key4AppRules(app);
        return (Map<Long, T>) cache.get(key);
    }

    @Override
    public List<T> saveAll(List<T> rules) {
        cache.clear();

        if (rules == null) {
            return null;
        }
        List<T> savedRules = new ArrayList<>(rules.size());
        for (T rule : rules) {
            savedRules.add(save(rule));
        }
        return savedRules;
    }

    @Override
    public T delete(Long id) {
        T entity = deleteAllRule(id);
        if (entity != null) {
            deleteAppRule(entity);
            deleteMachineRule(entity);
        }
        return entity;
    }

    @Override
    public T findById(Long id) {
        return findAllRule(id);
    }

    @Override
    public List<T> findAllByMachine(MachineInfo machineInfo) {
        Map<Long, T> entities = findMachineRule(machineInfo);
        if (entities == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(entities.values());
    }

    @Override
    public List<T> findAllByApp(String appName) {
        AssertUtil.notEmpty(appName, "appName cannot be empty");
        Map<Long, T> entities = findAppRule(appName);
        if (entities == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(entities.values());
    }

    public void clearAll() {
        cache.clear();
    }

    @Override
    protected final long nextId() {
        return igniteAtomicLong.incrementAndGet();
    }

    protected abstract String cacheName();

    @Override
    public void afterPropertiesSet() throws Exception {
        CacheConfiguration cacheCfg = new CacheConfiguration<>();
        cacheCfg.setName(cacheName());
        cacheCfg.setSqlSchema(BaseIgniteModel.SCHEMA);
        cacheCfg.setCacheMode(CacheMode.REPLICATED);
        cache = ignite.getOrCreateCache(cacheCfg);
        igniteAtomicLong = ignite.atomicLong(cacheName() + ".ID", 0, true);
    }
}
