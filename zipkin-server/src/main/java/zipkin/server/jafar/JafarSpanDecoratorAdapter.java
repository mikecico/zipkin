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

import static java.util.logging.Level.WARNING;
import static java.util.logging.Level.FINE;
//import static zipkin.server.jafar.JafarSpanEnhancer.getPodMetadata;

//import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import zipkin.collector.SpanDecorator;

abstract class JafarSpanDecoratorAdapter<S> implements SpanDecorator<S>, JafarDecoratorConstants {

  private final Logger logger;

  public JafarSpanDecoratorAdapter(Logger logger) {
    this.logger = logger;
  }

  @Override
  public List<S> decorate(Map<String, String> allHeaders, List<S> spans) {
    // Perform massaging here
    debugMessage("------- Input spans: ", spans);
    List<S> updatedSpans = spans;
    if (PASS_THROUGH) {
      debugMessage("PASS-THROUGH Mode, skip span enhancement");
    } else {
      final String podKey = extractPodKey(allHeaders);
//      Map<String, Object> podMetaData = getPodMetadata(podKey, VERBOSE_ENHANCEMENT);
//      Map<String, Object> podMetaData = new HashMap<String, Object>(2);
      if (podKey != null) {
        updatedSpans = enhanceSpans(spans, podKey); //podMetaData);
        debugMessage("+++++++++ Output spans: ", updatedSpans);
      } else {
        debugMessage("No podkey found, skipping Span enhancement");
      }
    }
    return updatedSpans;
  }
  
  void debugMessage(Object... msgParts) {
    if (msgParts != null && msgParts.length > 0 && logger.isLoggable(Level.FINE)){
      StringBuilder msgBuf = new StringBuilder();
      for (Object part : msgParts) {
        msgBuf.append(part.toString()).append(" ");
      }
      logger.log(FINE, msgBuf.toString());
    }
  }
  
  protected void warn(String message, Throwable e) {
    logger.log(WARNING, message, e);
  }

  protected String extractPodKey(Map<String, String> allHeaders) {
    String headerKey = ODX_HEADER_KEY; 
    final String usePodKey = allHeaders == null ? null : allHeaders.get(headerKey);
    debugMessage("PodKey: ", usePodKey);
    return usePodKey;
  }

  abstract protected List<S> enhanceSpans(List<S> spans, String podKey); //Map<String, Object> podMetaData);

  protected zipkin2.Span enhanceV2Span(String podKey, zipkin2.Span span) {
      zipkin2.Span.Builder span2Builder = span.toBuilder()
          .putTag(ODX_POD_KEY, podKey)
//          .putTag(ODX_ENHANCER_VERSION_KEY,ENHANCER_VERSION)
          ;
      //      for (Entry<String, Object> entry : podMetadata.entrySet()) {
  //        Object value = entry.getValue();
  //        span2Builder.putTag(entry.getKey(), value == null ? "" : value.toString());
  //      }
      zipkin2.Span updatedSpan2 = span2Builder.build();
      return updatedSpan2;
    }
}
