package com.lifeofcoder.autolimiter.dashboard.ignite;

import com.lifeofcoder.autolimiter.dashboard.ignite.dao.impl.IgniteMachineDao;
import com.lifeofcoder.autolimiter.dashboard.ignite.dao.impl.IgniteMetricsDao;
import com.lifeofcoder.autolimiter.dashboard.model.IgniteMachine;
import com.lifeofcoder.autolimiter.dashboard.model.IgniteMetrics;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.datasource.entity.MetricEntity;
import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.discovery.MachineInfo;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.failure.RestartProcessFailureHandler;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_PUT;
import static org.apache.ignite.events.EventType.EVT_CACHE_OBJECT_REMOVED;

public class IgniteFactoryBeanTest2 {

    public static void main(String[] args) {
        new IgniteFactoryBeanTest2().client();
    }

    public void client() {
        Ignite ignite = getAndStartIninte();
        try {
            initData4Machine(ignite);
            initData4Metric(ignite);
        }
        catch (InterruptedException e) {
            e.printStackTrace(System.out);
        }
        finally {
            ignite.close();
            System.out.println("OK");
        }
    }

    private void initData4Metric(Ignite ignite) throws InterruptedException {
        IgniteCache<Object, Object> cache = ignite.getOrCreateCache(IgniteMetricsDao.CACHE_NAME);
        MetricEntity metricEntity = new MetricEntity();
        IgniteMetrics igniteMetrics = new IgniteMetrics(metricEntity);
        metricEntity.setApp("m");
        metricEntity.setResource("query");
        metricEntity.setBlockQps(1L);
        metricEntity.setSuccessQps(1L);
        metricEntity.setPassQps(1L);
        metricEntity.setTimestamp(new Date());
        metricEntity.setGmtCreate(new Date());
        metricEntity.setGmtModified(new Date());
        cache.put("1", igniteMetrics);

        TimeUnit.SECONDS.sleep(2);
        metricEntity.setBlockQps(2L);
        metricEntity.setTimestamp(new Date());
        metricEntity.setGmtCreate(new Date());
        metricEntity.setGmtModified(new Date());
        cache.put("2", igniteMetrics);

        TimeUnit.SECONDS.sleep(2);
        metricEntity.setResource("modify");
        metricEntity.setBlockQps(2L);
        metricEntity.setTimestamp(new Date());
        metricEntity.setGmtCreate(new Date());
        metricEntity.setGmtModified(new Date());
        cache.put("3", igniteMetrics);
    }

    private void initData4Machine(Ignite ignite) {
        IgniteCache<Object, Object> cache = ignite.getOrCreateCache(IgniteMachineDao.CACHE_NAME);

        MachineInfo machineInfo = new MachineInfo();
        machineInfo.setApp("m");
        machineInfo.setPort(1);
        machineInfo.setIp("127.0.0.1");

        IgniteMachine igniteMachine = new IgniteMachine(machineInfo);
        cache.put("1", igniteMachine);

        machineInfo.setApp("m");
        machineInfo.setPort(2);
        machineInfo.setIp("127.0.0.3");
        cache.put("2", igniteMachine);

        machineInfo.setApp("pc");
        machineInfo.setPort(3);
        machineInfo.setIp("127.0.0.4");
        cache.put("3", igniteMachine);

        machineInfo.setApp("pc");
        machineInfo.setPort(4);
        machineInfo.setIp("127.0.0.4");
        cache.put("4", igniteMachine);

        machineInfo.setApp("bocai");
        machineInfo.setPort(4);
        machineInfo.setIp("127.0.0.4");
        cache.put("5", igniteMachine);
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
        //        ipFinder.setAddresses(Arrays.asList("11.19.84.96", "11.21.167.230", "11.37.83.249"));
        ipFinder.setAddresses(Arrays.asList("10.14.138.215"));
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