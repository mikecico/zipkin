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
package zipkin.server.jafar;

import java.util.ArrayList;
import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import zipkin.Span;
import zipkin.collector.SpanDecorator;
import zipkin.internal.V2SpanConverter;

@Service
@Qualifier(SpanDecorator.V1_QUALIFIER)
public class JafarV1SpanDecorator extends JafarSpanDecoratorAdapter<zipkin.Span> {

  public JafarV1SpanDecorator() {
    super(Logger.getLogger(JafarV1SpanDecorator.class.getSimpleName()));
  }
  
  @Override
  public boolean isV1SpanSupported() {
    return true;
  }
  
  @Override
  public boolean isV2SpanSupported() {
    return false;
  }
  
  @Override
  protected List<Span> enhanceSpans(List<Span> spans, String podKey) {
  //protected List<Span> enhanceSpans(List<Span> spans, Map<String, Object> podMetaData) {
//    podMetaData.put("Jafar-Enhancer-Version", ENHANCER_VERSION);
    List<Span> enhancedSpans = new ArrayList<Span>();
    for (Span inputSpan : spans) {
      List<zipkin.Span> annotatedSpans = enhanceSpan(inputSpan, podKey); //podMetaData);
      enhancedSpans.addAll(annotatedSpans);
    }
    return enhancedSpans;
  }
  
  protected List<zipkin.Span> enhanceSpan(zipkin.Span sourceSpan, String podKey) { //Map<String, Object> podMetadata) {
    List<zipkin2.Span> span2Copies = V2SpanConverter.fromSpan(sourceSpan); 
    List<zipkin.Span> convertedSpans = new ArrayList<zipkin.Span>();
    for (zipkin2.Span span : span2Copies) {
      zipkin2.Span enhancedSpan = enhanceV2Span(podKey, span);
      convertedSpans.add(V2SpanConverter.toSpan(enhancedSpan));   
    }
    return convertedSpans;
  }

}
