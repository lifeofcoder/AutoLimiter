package com.lifeofcoder.autolimiter.demo.client;

import com.lifeofcoder.autolimiter.demo.client.controller.TestController;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 定时请求
 *
 * @author xbc
 * @date 2020/7/28
 */
@Service
public class RequestTimer implements InitializingBean {
    ScheduledExecutorService scheduledExecutorService;
    @Autowired
    private TestController testController;

    public RequestTimer() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("start to execute.....");
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < getRandom(); i++) {
                        testController.mul();
                    }
                    for (int i = 0; i < getRandom(); i++) {
                        testController.hello();
                    }
                    for (int i = 0; i < getRandom(); i++) {
                        testController.auto();
                    }
                    for (int i = 0; i < getRandom(); i++) {
                        testController.aspect();
                    }
                }
                catch (Exception e) {
                    //ignore
                    e.printStackTrace(System.out);
                }
            }
        }, 10, 500, TimeUnit.MILLISECONDS);
    }

    private int getRandom() {
        return new Random().nextInt(100) + 100;
    }
}
