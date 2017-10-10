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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;

import zipkin.Span;
import zipkin.reporter.Reporter;
import zipkin.storage.AsyncSpanConsumer;
import zipkin.storage.Callback;
import zipkin2.Span.Builder;
import zipkin.internal.*;

//import static com.oracle.odx.jafar.zipkin.JafarSpanEnhancer.getPodMetadata;

public class JafarAsyncSpanConsumer implements AsyncSpanConsumer {

  private static final String ENHANCER_VERSION = "0.2";
  
  // Option to skip the span-enhancement for debugging purposes; if set
  // Spans will be passed through without modification
  private static final Boolean PASS_THROUGH = Boolean.getBoolean("jafar.proxy.passthrough");
  private static final boolean VERBOSE_ENHANCEMENT = Boolean.getBoolean("jafar.proxy.include.details");
  private static final Boolean STORE_ENHANCED_SPANS = Boolean.getBoolean("jafar.proxy.store.raw");
  
  private Executor executor;
  //private Reporter<Span> reporter;
  private AsyncSpanConsumer delegate;
  
  public JafarAsyncSpanConsumer(Executor executor, Reporter<Span> reporter, AsyncSpanConsumer delegate) {
    System.out.println("+++++++++ Creating JafarProxy AsyncSpanConsumer");
    this.executor = executor;
    //this.reporter = reporter; 
    this.delegate = delegate;
    
    System.out.println("+++++++++ PASS_THROUGH: " + PASS_THROUGH);
    System.out.println("+++++++++ VERBOSE_ENHANCEMENT: " + VERBOSE_ENHANCEMENT);
    System.out.println("+++++++++ STORE_ENHANCED_SPANS: " + STORE_ENHANCED_SPANS);
  }

  @Override
  public void accept(List<Span> spans, Callback<Void> callback) {
    
    System.out.println("+++++++++ JAFAR received spans: " + spans);
    
//    String podKey = "UKNOWN:UNKNOWN:UNKNOWN";
//    Object callbackObject = callback.getCallbackObject();
//    if (callbackObject != null && callbackObject instanceof String) {
//      podKey = (String) callbackObject;
//    }
    
//    System.out.println("+++++++++ JAFAR callback object value: " + callbackObject);
    
//    Map<String, Object> podMetaData = getPodMetadata(podKey, VERBOSE_ENHANCEMENT);
//    podMetaData.put("Jafar-Enhancer-Version", ENHANCER_VERSION);
//    
    List<Span> modifiedSpans = new ArrayList<Span>(spans.size());
//    if (PASS_THROUGH) {
//      System.out.println("------- PASS-THROUGH Mode, skip span enhancement");
//      modifiedSpans.addAll(spans);
//    } else {
//      for (Span sourceSpan : spans) {
//        System.out.println("------- Input span: " + sourceSpan);
//        List<Span> annotatedSpans = enhanceSpan(sourceSpan, podMetaData);
//        displayConvertedSpans(annotatedSpans);
//        modifiedSpans.addAll(annotatedSpans);
//      }
//    }
    
    if (delegate != null) {
      try {
        List<Span> spanToStore = STORE_ENHANCED_SPANS ? modifiedSpans: spans;
        String msg = STORE_ENHANCED_SPANS ? "enhanced" : "unmodified";
        System.out.println("+++++++++ Storing " + msg + " spans locally");
        delegate.accept(spanToStore, callback);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    callback.onSuccess(null);
  }

//  private void displayConvertedSpans(List<Span> annotatedSpans) {
//    System.out.println("+++++++ Output spans: " + annotatedSpans);
//    for (Span annotatedSpan : annotatedSpans) {
//      System.out.println("\t" + annotatedSpan);
//    }
//  }
//
//  private List<Span> enhanceSpan(Span sourceSpan, Map<String, Object> podMetadata) {
//    List<zipkin2.Span> span2Copies = V2SpanConverter.fromSpan(sourceSpan); 
//    List<Span> convertedSpans = new ArrayList<Span>();
//    for (zipkin2.Span span : span2Copies) {
//      Builder span2Builder = span.toBuilder();
//      for (Entry<String, Object> entry : podMetadata.entrySet()) {
//        Object value = entry.getValue();
//        span2Builder.putTag(entry.getKey(), value == null ? "" : value.toString());
//      }
//      zipkin2.Span updatedSpan2 = span2Builder.build();
//      //System.out.println("\t\tspan2 with tags:" + updatedSpan2.toString());
//      convertedSpans.add(V2SpanConverter.toSpan(updatedSpan2));   
//    }
//    return convertedSpans;
//  }

}
