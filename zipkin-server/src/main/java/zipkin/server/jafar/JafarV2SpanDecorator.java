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

import zipkin.collector.SpanDecorator;
import zipkin2.Span;
//import zipkin2.Span.Builder;

@Service
@Qualifier(SpanDecorator.V2_QUALIFIER)
public class JafarV2SpanDecorator extends JafarSpanDecoratorAdapter<Span> {

  public JafarV2SpanDecorator() {
    super(Logger.getLogger(JafarV2SpanDecorator.class.getSimpleName()));
  }

  @Override
  public boolean isV1SpanSupported() {
    return false;
  }
  
  @Override
  public boolean isV2SpanSupported() {
    return true;
  }
  
  @Override
  //protected List<Span> enhanceSpans(List<Span> spans, Map<String, Object> podMetaData) {
  protected List<Span> enhanceSpans(List<Span> spans, String podKey) {
    List<Span> enhancedSpans = new ArrayList<Span>(spans.size());
    for (Span span : spans) {
      debugMessage("------- Input span: " + span);
//      Builder enhSpanBuilder = span.toBuilder().putTag(ODX_POD_KEY, podKey).putTag(ODX_ENHANCER_VERSION_KEY,
//          ENHANCER_VERSION);
//      //      for (Entry<String, Object> entry : podMetaData.entrySet()) {
////        Object value = entry.getValue();
////        enhSpanBuilder.putTag(entry.getKey(), value == null ? "" : value.toString());
////      }
//      Span enhancedSpan = enhSpanBuilder.build();
      zipkin2.Span enhancedSpan = enhanceV2Span(podKey, span);
      debugMessage("+++++++ Output span: " + enhancedSpan);
      enhancedSpans.add(enhancedSpan);
    }
    return enhancedSpans;
  }
}
