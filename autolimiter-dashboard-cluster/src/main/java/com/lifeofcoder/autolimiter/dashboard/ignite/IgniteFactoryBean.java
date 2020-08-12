package com.lifeofcoder.autolimiter.dashboard.ignite;

import org.apache.commons.lang.StringUtils;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DeploymentMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.failure.RestartProcessFailureHandler;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Ignite的构造工厂
 *
 * @author xbc
 * @date 2020/7/15
 */
@Service
public class IgniteFactoryBean extends AbstractFactoryBean<Ignite> {
    //    private final static Logger LOGGER = LoggerFactory.getLogger(IgniteFactoryBean.class);

    private Ignite ignite;

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${ignite.register.addresses}")
    private String igniteRegisterAddresses;

    private Ignite getAndStartIninte() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        //设为false表示服务端模式
        cfg.setClientMode(false);
        //分布式计算class传播
        cfg.setPeerClassLoadingEnabled(true);
        //部署模式，控制类加载
        cfg.setDeploymentMode(DeploymentMode.SHARED);
        //禁用丢失资源缓存
        cfg.setPeerClassLoadingMissedResourcesCacheSize(0);
        //连接超时时间
        cfg.setNetworkTimeout(10000);
        cfg.setFailureHandler(new RestartProcessFailureHandler());
        //公共线程池大小
        cfg.setPublicThreadPoolSize(32);
        //系统线程池大小
        cfg.setSystemThreadPoolSize(16);

        //TCP Discovery
        TcpDiscoverySpi tcpDiscoverSpi = new TcpDiscoverySpi();
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        //通过组播发现
        //        ipFinder.setMulticastGroup("224.0.0.251");

        //通过静态IP发现，可以和组播同时开启
        ipFinder.setAddresses(Arrays.asList(StringUtils.split(igniteRegisterAddresses, ",")));
        cfg.setDiscoverySpi(tcpDiscoverSpi);
        tcpDiscoverSpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(tcpDiscoverSpi);

        BinaryConfiguration bc = new BinaryConfiguration();
        bc.setCompactFooter(true);
        cfg.setBinaryConfiguration(bc);

        cfg.setGridLogger(new org.apache.ignite.logger.slf4j.Slf4jLogger());

        //全局缓存设置
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        // 缓存名
        cacheConfiguration.setName("IgniteCache");
        //原子模式类型，ATOMIC:原子型，保证性能; TRANSACTIONAL:事务型,分布式锁
        cacheConfiguration.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        //PARTITIONED:分区; REPLICATED:复制；LOCAL：本地
        cacheConfiguration.setCacheMode(CacheMode.PARTITIONED);
        //备份数量(副本数)
        cacheConfiguration.setBackups(1);
        //禁用jcache标准中缓存读取获取的是副本的机制
        cacheConfiguration.setCopyOnRead(false);
        //内存区名(配置的DataRegionConfiguration必须存在)，没有开持久化并不需要配置这个。
        //        cacheConfiguration.setDataRegionName("IgniteDataRegion");
        //是否以二进制形式存储
        cacheConfiguration.setStoreKeepBinary(true);
        cfg.setCacheConfiguration(cacheConfiguration);

        //因为不持久化数据，所以没设置DataStorageConfiguration
        //        DataStorageConfiguration dataStorageCfg = new DataStorageConfiguration();
        //        dataStorageCfg.setWalMode(WALMode.NONE);
        //        DataRegionConfiguration defaultDataRegionCfg = new DataRegionConfiguration();
        //        defaultDataRegionCfg.setPersistenceEnabled(false);
        //        defaultDataRegionCfg.setName("default1");
        //        dataStorageCfg.setDefaultDataRegionConfiguration(defaultDataRegionCfg);
        //        dataStorageCfg.setDefaultDataRegionConfiguration(defaultDataRegionCfg);
        //
        //        DataRegionConfiguration dataRegionCfg = new DataRegionConfiguration();
        //        dataRegionCfg.setName("IgniteDataRegion");
        //        dataRegionCfg.setPersistenceEnabled(false);
        //        dataStorageCfg.setDataRegionConfigurations(dataRegionCfg);
        //        cfg.setDataStorageConfiguration(dataStorageCfg);

        //开启对那些事件感兴趣,这里面有很多事件
        cfg.setIncludeEventTypes(EventType.EVTS_DISCOVERY);
        Ignite ignite = Ignition.start(cfg);
        return ignite;
    }

    @Override
    public Class<?> getObjectType() {
        return Ignite.class;
    }

    @Override
    protected Ignite createInstance() throws Exception {
        ignite = getAndStartIninte();
        ignite.cluster().baselineAutoAdjustEnabled(false);
        ignite.cluster().active(true);
        ignite.cluster().setBaselineTopology(ignite.cluster().topologyVersion());
        return ignite;
    }
}
