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
package zipkin.collector;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Represents a service that can inspect and/or inject additional information
 * into spans prior to submission into a <code>StorageComponent</code>. At
 * present it can only be used in conjunction with the HTTP collector; if other
 * collectors (e.g., Kafka, Scribe, RabbitMQ) are in play, those collectors will
 * not utilize any SpanDecorators that might be available.
 * </p>
 * 
 * <p>
 * A <code>SpanDecorator</code> can Support either v1 or v2 versions of spans;
 * this is dictated implicitly by what is required by the
 * <code>StorageComponent</code> in use by Zipkin at runtime.
 * </p>
 * 
 * <p>
 * In order to implement a <code>SpanDecorator</code>, an class must implement
 * this interface and be tagged with the {@literal @}Service or
 * {@literal @}Component stereotype. The implementation must also indicate what
 * version of spans are supported by it through an {@literal @}Qualifier
 * annotation, e.g.,
 * </p>
 * 
 * <pre>
 * {@literal @}Service
 * {@literal @}Qualifier(SpanDecorator.V1_QUALIFIER)
 * public class JafarV1SpanDecorator extends JafarSpanDecoratorAdapter<zipkin.Span> {
 *   ...
 * }
 * </pre>
 * 
 * @param <S>
 *          The span-type supported by the decorator impl
 * @see zipkin.Span
 * @see zipkin2.Span
 */
public interface SpanDecorator<S> {
  
  /**
   * Qualifier value for v1 span support.
   */
  public static final String V1_QUALIFIER = "v1";
  
  /**
   * Qualifier value for v2 span support.
   */
  public static final String V2_QUALIFIER = "v2";
  
  /**
   * Decorate a span, possibly using the request info accompanying the
   * spans.  The implementation should perform any additional
   * span transformations and return them to the caller. 
   * 
   * @param requestInfo
   * @param spans
   * @return
   */
  List<S> decorate(Map<String, String> requestInfo, List<S> spans);

  /**
   * Indicates if the decorator supports V1 spans.
   * @return true if V1 spans are supported
   * @see zipkin.Span
   * @see zipkin2.Span
   */
  boolean isV1SpanSupported();
  
  /**
   * Indicates if the decorator supports V2 spans.
   * @return true if V2 spans are supported
   * @see zipkin.Span
   * @see zipkin2.Span
   */
  boolean isV2SpanSupported();
}
