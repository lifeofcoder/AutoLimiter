package com.lifeofcoder.autolimiter.dashboard.ignite;

import com.lifeofcoder.autolimiter.dashboard.JsonUtils;
import com.lifeofcoder.autolimiter.dashboard.ignite.dao.impl.IgniteMachineDao;
import com.lifeofcoder.autolimiter.dashboard.ignite.dao.impl.IgniteMetricsDao;
import com.lifeofcoder.autolimiter.dashboard.mapping.FieldWrapper;
import com.lifeofcoder.autolimiter.dashboard.mapping.MappingUtil;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.datasource.entity.MetricEntity;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.failure.RestartProcessFailureHandler;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

import java.util.*;

import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_PUT;
import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_REMOVED;

public class IgniteFactoryBeanTest {

    public static void main(String[] args) {
        new IgniteFactoryBeanTest().client();
    }

    public void client() {
        Ignite ignite = getAndStartIninte();
        try {
            for (ClusterNode node : ignite.cluster().forServers().forOldest().nodes()) {
                System.out.println(node.addresses());
            }
            getMachine(ignite);
            getMetrics(ignite);
        }
        finally {
            ignite.close();
            System.out.println("OK");
        }
    }

    private void getMachine(Ignite ignite) {
        System.out.println("==========machine=============");
        IgniteCache<Object, Object> cache = ignite.getOrCreateCache(IgniteMachineDao.CACHE_NAME);

        StringBuilder sqlBuilder = new StringBuilder("select * from machine");
        queryAndPrint(cache, sqlBuilder);
    }

    private void getMetrics(Ignite ignite) {
        System.out.println("==========metrics=============");
        IgniteCache<Object, Object> cache = ignite.getOrCreateCache(IgniteMetricsDao.CACHE_NAME);

        String app = "sentinel-ignite";
        String resource = "query";
        long startTime = new Date().getTime() - 1000 * 60 * 60;
        long endTime = new Date().getTime();
        StringBuilder sqlBuilder = new StringBuilder("select * from metrics where app='");
        sqlBuilder.append(app).append("'");
        //.append(" and resource='").append(resource).append("'");
        //sqlBuilder.append(" and timestamp >= ").append(startTime).append(" and timestamp <= ").append(endTime);
        //        List<MetricEntity> query = query(cache, new SqlFieldsQuery(sqlBuilder.toString()), MetricEntity.class);
        //        for (MetricEntity metricEntity : query) {
        //            System.out.println(JsonUtils.toJson(metricEntity));;
        //        }
        //        queryAndPrint(cache, sqlBuilder);
        queryAndPrint(cache, sqlBuilder, MetricEntity.class);
    }

    private <T> void queryAndPrint(IgniteCache cache, StringBuilder sqlBuilder, Class<T> cls) {
        List<T> query = query(cache, new SqlFieldsQuery(sqlBuilder.toString()), cls);
        for (T obj : query) {
            System.out.println(JsonUtils.toJson(obj));
        }
    }

    private void queryAndPrint(IgniteCache cache, StringBuilder sqlBuilder) {
        List<Map<String, Object>> query = query(cache, new SqlFieldsQuery(sqlBuilder.toString()));

        for (Map<String, Object> appName : query) {
            for (Map.Entry<String, Object> entry : appName.entrySet()) {
                System.out.print(entry.getKey() + ":" + entry.getValue() + ", ");
            }
            System.out.println("");
        }
    }

    public List<Map<String, Object>> query(IgniteCache cache, SqlFieldsQuery query) {
        FieldsQueryCursor<List<?>> queryCursor = cache.query(query);
        List<Map<String, Object>> result = new ArrayList<>();
        int column = queryCursor.getColumnsCount();
        Iterator it = queryCursor.iterator();
        while (it.hasNext()) {
            Map<String, Object> map = new HashMap<>();
            List<?> data = (List<?>) it.next();
            for (int i = 0; i < column; i++) {
                map.put(queryCursor.getFieldName(i), data.get(i));
            }
            result.add(map);
        }
        return result;
    }

    public <T> List<T> query(IgniteCache cache, SqlFieldsQuery query, Class<T> cls) {
        FieldsQueryCursor<List<?>> queryCursor = cache.query(query);
        List<Map<String, Object>> result = new ArrayList<>();
        int column = queryCursor.getColumnsCount();
        Iterator it = queryCursor.iterator();
        FieldWrapper fieldWrapper = MappingUtil.getOrCreate(cls);

        List<T> objList = new ArrayList<>();
        while (it.hasNext()) {
            List<?> data = (List<?>) it.next();
            T obj = null;
            try {
                obj = cls.newInstance();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < column; i++) {
                fieldWrapper.set(obj, queryCursor.getFieldName(i), data.get(i));
            }
            objList.add(obj);
        }

        return objList;
    }

    private static Ignite getAndStartIninte() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIncludeEventTypes(EVT_CACHE_OBJECT_PUT, EVT_CACHE_OBJECT_REMOVED);
        //设为false表示服务端模式
        cfg.setClientMode(true);
        cfg.setIgniteInstanceName("client");
        //分布式计算class传播
        cfg.setPeerClassLoadingEnabled(true);
        //连接超时时间
        cfg.setNetworkTimeout(10000);
        cfg.setFailureHandler(new RestartProcessFailureHandler());

        //TCP Discovery
        TcpDiscoverySpi tcpDiscoverSpi = new TcpDiscoverySpi();
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        //通过组播发现
        //        ipFinder.setMulticastGroup("228.10.10.157");
        //通过静态IP发现，可以和组播同时开启
        ipFinder.setAddresses(Arrays.asList("11.19.84.96", "11.21.167.230", "11.37.83.249"));
        //        ipFinder.setAddresses(Arrays.asList("10.14.138.215"));
        cfg.setDiscoverySpi(tcpDiscoverSpi);
        tcpDiscoverSpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(tcpDiscoverSpi);

        BinaryConfiguration bc = new BinaryConfiguration();
        bc.setCompactFooter(true);
        cfg.setBinaryConfiguration(bc);

        cfg.setGridLogger(new org.apache.ignite.logger.slf4j.Slf4jLogger());

        Ignite ignite = Ignition.getOrStart(cfg);
        return ignite;
    }
}