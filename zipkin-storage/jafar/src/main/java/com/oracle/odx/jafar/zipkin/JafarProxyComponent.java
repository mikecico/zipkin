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

import static zipkin.internal.Util.checkNotNull;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import zipkin.Span;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.Reporter;
import zipkin.reporter.urlconnection.URLConnectionSender;
import zipkin.storage.AsyncSpanConsumer;
import zipkin.storage.AsyncSpanStore;
import zipkin.storage.InMemoryStorage;
import zipkin.storage.SpanStore;
import zipkin.storage.StorageComponent;

@Configuration
@EnableConfigurationProperties(JafarProxyStorageProperties.class)
@ConditionalOnProperty(name = "zipkin.storage.type", havingValue = "jafar")
@ConditionalOnMissingBean(StorageComponent.class)
public class JafarProxyComponent implements StorageComponent {

  private static final String REMOTE_AGENT_URL = System.getProperty("jafar.remote.agent",
      "http://localhost:9511");

  private static final String REMOTE_AGENT_ENDPOINT = REMOTE_AGENT_URL+
      System.getProperty("jafar.remote.agent.path", "/api/v1/spans");
  
  private Executor executor = new ScheduledThreadPoolExecutor(1);
  
  private Reporter<Span> reporter;

  private URLConnectionSender sender;

  private InMemoryStorage memStore;

  public static class Builder implements StorageComponent.Builder {
    
    private Executor executor;

    private Builder() {}
    
    @Override
    public StorageComponent build() {
      return new JafarProxyComponent(this);
    }

    @Override
    public zipkin.storage.StorageComponent.Builder strictTraceId(boolean strictTraceId) {
      // TODO Auto-generated method stub
      return null;
    }

    public Builder executor(Executor executor) {
      this.executor = checkNotNull(executor, "executor");
      return this;
    }
  }
  
  public JafarProxyComponent(Builder builder) {
    executor = builder.executor;
    System.out.println("++++++++++++ Creating Jafar Storage Component");
    String targetEndpoint = REMOTE_AGENT_ENDPOINT;
    try {
//      System.out.println("Creating sender for remote agent " + REMOTE_AGENT_ENDPOINT);
//      sender = URLConnectionSender.create(targetEndpoint);
//      reporter = AsyncReporter.create(sender);
    } catch (Throwable t) {
      System.out.println("Error initializing reporter to target endpoint " + targetEndpoint);
      t.printStackTrace();
    }
    memStore = InMemoryStorage.builder().build();
  }

  @Override
  public CheckResult check() {
    if (sender != null) 
      return sender.check();
    return CheckResult.OK;
  }

  @Override
  public void close() throws IOException {
    if (sender != null)
      sender.close();
  }

  @Override
  public AsyncSpanConsumer asyncSpanConsumer() {
    return new JafarAsyncSpanConsumer(executor, reporter, memStore.asyncSpanConsumer());
  }

  @Override
  public SpanStore spanStore() {
    return memStore.spanStore();
  }

  @Override
  public AsyncSpanStore asyncSpanStore() {
    return memStore.asyncSpanStore();
  }

  public static Builder builder() {
    return new Builder();
  }
}
