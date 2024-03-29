/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lifeofcoder.autolimiter.cluster.server;

import com.alibaba.csp.sentinel.cluster.TokenService;
import com.alibaba.csp.sentinel.util.SpiLoader;
import com.lifeofcoder.autolimiter.cluster.flow.DefaultTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public final class TokenServiceProvider {
    private static Logger LOGGER = LoggerFactory.getLogger(TokenServiceProvider.class);

    private static TokenService service = null;

    static {
        resolveTokenServiceSpi();
    }

    public static TokenService getService() {
        return service;
    }

    private static void resolveTokenServiceSpi() {
        service = SpiLoader.loadFirstInstanceOrDefault(TokenService.class, DefaultTokenService.class);
        if (service != null) {
            LOGGER.info("[TokenServiceProvider] Global token service resolved: " + service.getClass().getCanonicalName());
        }
        else {
            LOGGER.warn("[TokenServiceProvider] Unable to resolve TokenService: no SPI found");
        }
    }
}
