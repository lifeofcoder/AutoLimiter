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
package com.lifeofcoder.autolimiter.cluster.common.response;

/**
 * Cluster transport response interface.
 *
 * @author Eric Zhao
 * @since 1.4.0
 */
public interface Response {
    /**
     * Get response ID.
     *
     * @return response ID
     */
    int getId();

    /**
     * Get response type.
     *
     * @return response type
     */
    int getType();

    /**
     * Get response status.
     *
     * @return response status
     */
    int getStatus();
}
