package com.lifeofcoder.autolimiter.demo.client;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.lifeofcoder.autolimiter.client.ConfigListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SentinelClientApplication extends SpringBootServletInitializer {

    @Value("${autolimiter.dashboard.addresses}")
    private String dashboardAddrs;

    public static void main(String[] args) {
        SpringApplication.run(SentinelClientApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // 注意这里要指向原先用main方法执行的Application启动类
        return builder.sources(SentinelClientApplication.class);
    }

    @Bean
    public SentinelResourceAspect limiterResourceAspect() {
        return new SentinelResourceAspect();
    }

    @Bean
    public ConfigListener configListener() {
        return new ConfigListener(dashboardAddrs);
    }
}
