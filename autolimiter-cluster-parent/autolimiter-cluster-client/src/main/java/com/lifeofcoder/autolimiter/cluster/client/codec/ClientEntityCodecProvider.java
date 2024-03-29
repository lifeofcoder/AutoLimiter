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
package com.lifeofcoder.autolimiter.cluster.client.codec;

import com.alibaba.csp.sentinel.util.SpiLoader;
import com.lifeofcoder.autolimiter.cluster.common.codec.request.RequestEntityWriter;
import com.lifeofcoder.autolimiter.cluster.common.codec.response.ResponseEntityDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Eric Zhao
 * @since 1.4.0
 */
public final class ClientEntityCodecProvider {
    private static Logger LOGGER = LoggerFactory.getLogger(ClientEntityCodecProvider.class);

    private static RequestEntityWriter requestEntityWriter = null;
    private static ResponseEntityDecoder responseEntityDecoder = null;

    static {
        resolveInstance();
    }

    private static void resolveInstance() {
        RequestEntityWriter writer = SpiLoader.loadFirstInstance(RequestEntityWriter.class);
        if (writer == null) {
            LOGGER.warn("[ClientEntityCodecProvider] No existing request entity writer, resolve failed");
        }
        else {
            requestEntityWriter = writer;
            LOGGER.info("[ClientEntityCodecProvider] Request entity writer resolved: " + requestEntityWriter.getClass().getCanonicalName());
        }
        ResponseEntityDecoder decoder = SpiLoader.loadFirstInstance(ResponseEntityDecoder.class);
        if (decoder == null) {
            LOGGER.warn("[ClientEntityCodecProvider] No existing response entity decoder, resolve failed");
        }
        else {
            responseEntityDecoder = decoder;
            LOGGER.info("[ClientEntityCodecProvider] Response entity decoder resolved: " + responseEntityDecoder.getClass().getCanonicalName());
        }
    }

    public static RequestEntityWriter getRequestEntityWriter() {
        return requestEntityWriter;
    }

    public static ResponseEntityDecoder getResponseEntityDecoder() {
        return responseEntityDecoder;
    }

    private ClientEntityCodecProvider() {
    }
}
