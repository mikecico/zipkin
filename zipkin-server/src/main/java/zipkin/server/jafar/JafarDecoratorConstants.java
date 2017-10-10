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

public interface JafarDecoratorConstants {

  // Option to skip the span-enhancement for debugging purposes; if set
  // Spans will be passed through without modification
  static final Boolean PASS_THROUGH = Boolean.getBoolean("jafar.proxy.passthrough");
  static final boolean VERBOSE_ENHANCEMENT = Boolean.getBoolean("jafar.proxy.include.details");

  static final String ENHANCER_VERSION = "0.3-es";
  static final String ODX_HEADER_KEY = "X-ODX-PodKey".toLowerCase();
  static final String ODX_POD_KEY = "ODX-Pod-Key";
  static final String ODX_ENHANCER_VERSION_KEY = "ODX-Enhancer-Version";

}
