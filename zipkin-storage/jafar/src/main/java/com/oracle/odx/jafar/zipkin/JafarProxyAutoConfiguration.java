/**
 * Copyright 2015-2017 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.oracle.odx.jafar.zipkin;

import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import zipkin.storage.StorageComponent;

@Configuration
@EnableConfigurationProperties(JafarProxyStorageProperties.class)
@ConditionalOnProperty(name = "zipkin.storage.type", havingValue = "jafar")
@ConditionalOnMissingBean(StorageComponent.class)
public class JafarProxyAutoConfiguration {
  @Autowired(required = false)
  JafarProxyStorageProperties jafar;

//  @Autowired(required = false)
//  @Qualifier("tracingExecuteListenerProvider")
//  ExecuteListenerProvider listener;

  @Bean @ConditionalOnMissingBean(Executor.class) Executor executor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setThreadNamePrefix("ZipkinJafarProxy-");
    executor.initialize();
    return executor;
  }

  @Bean
  StorageComponent storage(Executor executor, @Value("${zipkin.storage.strict-trace-id:true}") boolean strictTraceId) {
    return JafarProxyComponent.builder().executor(executor).build();
  }
}
