package com.lifeofcoder.autolimiter.dashboard.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用于初始化配置
 *
 * @author xbc
 * @date 2020/7/13
 */
@Component
public class ConfigInitiateEventListener implements ApplicationListener<ApplicationReadyEvent>, InitializingBean {
    private static Logger LOGGER = LoggerFactory.getLogger(ConfigInitiateEventListener.class);

    @Autowired
    private ApplicationContext applicationContext;

    private List<Starter> starterList;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        for (Starter starter : starterList) {
            starter.start();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Starter> beansOfType = applicationContext.getBeansOfType(Starter.class);
        starterList = new ArrayList<>(beansOfType.size());
        starterList.addAll(beansOfType.values());
    }
}