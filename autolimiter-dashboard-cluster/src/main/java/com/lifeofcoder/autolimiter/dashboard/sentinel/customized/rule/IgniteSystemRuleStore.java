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
package com.lifeofcoder.autolimiter.dashboard.sentinel.customized.rule;

import com.lifeofcoder.autolimiter.dashboard.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class IgniteSystemRuleStore extends IgniteRuleRepositoryAdapter<SystemRuleEntity> {
    @Override
    protected String cacheName() {
        return "SystemRuleEntity";
    }
}
