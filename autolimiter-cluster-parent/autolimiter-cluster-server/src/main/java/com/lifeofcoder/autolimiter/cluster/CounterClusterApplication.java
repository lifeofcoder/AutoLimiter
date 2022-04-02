package com.lifeofcoder.autolimiter.cluster;

import com.lifeofcoder.autolimiter.cluster.server.SentinelDefaultTokenServer;
import com.lifeofcoder.autolimiter.cluster.server.config.ClusterServerConfigManager;
import com.lifeofcoder.autolimiter.cluster.server.config.ServerFlowConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.IOException;

/**
 * 计数集群
 * 由tomcat加载ServletContainerInitializer，最后启动SpringBootServletInitializer
 */
@EnableAspectJAutoProxy
@SpringBootApplication
public class CounterClusterApplication extends SpringBootServletInitializer {
    private static Logger LOGGER = LoggerFactory.getLogger(CounterClusterApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CounterClusterApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // 注意这里要指向原先用main方法执行的Application启动类
        return builder.sources(CounterClusterApplication.class);
    }

    /**
     * 启动服务端
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public SentinelDefaultTokenServer sentinelDefaultTokenServer() throws IOException {
        SentinelDefaultTokenServer token = new SentinelDefaultTokenServer();
        //配置
        //TODO 改造成SpringBoot配置，基于Ducc配置实现动态更新
        ClusterServerConfigManager.loadGlobalFlowConfig(new ServerFlowConfig());
        //        ClusterServerConfigManager.loadServerNamespaceSet(new HashSet<>());
        //        ClusterServerConfigManager.loadFlowConfig("nameSpace", new ServerFlowConfig());
        //配置移动到DUCC了
        //        ClusterServerConfigManager.loadGlobalTransportConfig(new ServerTransportConfig());
        LOGGER.info("The server has started successfully.");
        return token;
    }
}