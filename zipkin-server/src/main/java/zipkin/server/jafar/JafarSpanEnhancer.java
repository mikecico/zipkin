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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.*;

//import static java.util.logging.Level.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

//import io.netty.handler.logging.LogLevel;

public class JafarSpanEnhancer implements JafarDecoratorConstants {
  
  private static final String CONTAINER_METADATA_URL_PATH = "/container-metadata.json";
  private static final String POD_METADATA_PROPERTIES_URL_PATH = "/pod-metadata.properties";

  private static final String GENERATED_POD_METADATA_PATH = "./container-metadata.json";
  private static final String POD_METADATA_PROPERTIES_PATH = "./pod-metadata.properties";
  
//  private static final String POD_METADATA_SCRIPT_PATH = "/jafar-lib/generateK8SMetadata.sh";
  
  private static final Logger logger = Logger.getLogger("JafarSpanEnhancer");
  
//  private static final HashMap<String, Object> EMPTY_MAP = new HashMap<>(0);
  
  public static final String POD_IP = "podIP";
  public static final String POD_NAME = "podName";
  public static final String POD_NAMESPACE = "podNamespace";
  public static final String CONTAINER_NAME_ID_MAP = "containerNameIdMap";
  public static final String CONTAINER_PORT_NAME_MAP = "containerPortNameMap";

  // TODO: use WeakReference values and clean up the map periodically?
  static private Map<String,Map<String,Object>> podInfoCache = new ConcurrentHashMap<String, Map<String,Object>>();
  
  /**
   * Likely just a prototype; we'd likely have to listen for or query for pods coming and
   * going in order to prevent the cached Map from growing out of bounds over time.
   * 
   * @param podKey
   * @param includeDetails
   * @return
   */
  public static Map<String, Object> getPodMetadata(final String podKey, boolean includeDetails) {
    Map<String, Object> podInfoMap = null;
    if (podKey != null) {
      Map<String, Object> cachedPodInfo = podInfoCache.get(podKey);
      if (cachedPodInfo == null || cachedPodInfo.isEmpty()) {
        debugMessage("+++++++++ Cache MISS for podKey: " + podKey);
        String[] podKeyList = podKey.split(":");
        if (podKeyList.length == 3) {
          String nodeName = podKeyList[0];
          String podName = podKeyList[1];
          String podIP = podKeyList[2];
          debugMessage("+++++++++ Pod name: " + podName + ", node: " + nodeName + ", pod IP: " + podIP);
          StringBuilder baseURL = new StringBuilder().append("http://").append(podIP).append(":9411/podinfo");
          cachedPodInfo = buildcontainerMetadata(baseURL.toString(), includeDetails);
          podInfoCache.put(podKey, cachedPodInfo);
        } else {
          logger.warning("Invalid pod key: " + podKey);
        }
      } else {
        debugMessage("Cache HIT for pod key " + podKey + ": " + cachedPodInfo);
      }
      podInfoMap = new HashMap<>(cachedPodInfo);
    } else {
      debugMessage("Podkey was null");
      podInfoMap = new HashMap<>();
    }
    podInfoMap.put(ODX_ENHANCER_VERSION_KEY, ENHANCER_VERSION);
    podInfoMap.put(ODX_POD_KEY, podKey);
    return podInfoMap;
  }
  
  public static String readPodInfo(String baseURL) throws IOException {
    String podInfoURL = new StringBuilder(baseURL.toString()).append(CONTAINER_METADATA_URL_PATH).toString();
    URL podURL = new java.net.URL(podInfoURL);
    URLConnection conn = podURL.openConnection();
    Reader inputStreamReader = new InputStreamReader(
                                conn.getInputStream());
    return readToString(inputStreamReader).toString();   
  }

  private static Properties readPodProperties(String baseURL) throws IOException {
    String podPropsURL = new StringBuilder(baseURL.toString()).append(POD_METADATA_PROPERTIES_URL_PATH).toString();
    URL podURL = new URL(podPropsURL);
    URLConnection conn = podURL.openConnection();
    Reader inputStreamReader = new InputStreamReader(
                                conn.getInputStream());
    return readProps(inputStreamReader);
  }

  private static Properties readProps(Reader inputStreamReader) throws IOException {
    StringBuilder builder = readToString(inputStreamReader);
    Properties p = new Properties();
    p.load(new StringReader(builder.toString()));
    return p;
  }

  private static StringBuilder readToString(Reader inputStreamReader) throws IOException {
    BufferedReader in = new BufferedReader(inputStreamReader);
    StringBuilder builder = new StringBuilder();
    try {
      String inputLine;
      while ((inputLine = in.readLine()) != null)
        builder.append(inputLine).append("\n");
    } finally {
      in.close();
    }
    return builder;
  }

  private static Map<String,Object> buildcontainerMetadata(String baseURL, boolean includeDetails) {

    Map<Long,String> containerPortNameMap = new HashMap<Long, String>();
    Map<String,String> containerNameIdMap = new HashMap<String, String>();

    Properties podProps = new Properties();
    try {
      Object obj = null;
      if (baseURL == null) {
        // For debugging, read from a canned path
        
        String input = GENERATED_POD_METADATA_PATH;   
        logger.info("Reading pod info from " + input);
        obj = new JSONParser().parse(new FileReader(input));
        
        Properties readPodProperties = readProps(new FileReader(POD_METADATA_PROPERTIES_PATH));
        podProps.putAll(readPodProperties);

      } else {
        logger.info("Reading new pod info from " + baseURL);
        String jsonMap = readPodInfo(baseURL);
        obj = new JSONParser().parse(new StringReader(jsonMap));    
        
        Properties readPodProperties = readPodProperties(baseURL);
        podProps.putAll(readPodProperties);
      }
      
      JSONObject jo = (JSONObject) obj;
      JSONArray containers = (JSONArray) jo.get("containers");
      JSONArray containerStatuses = (JSONArray) jo.get("containerStatuses");
      
      for (Object item : containers) {
        JSONObject container = (JSONObject) item;
        String name = (String) container.get("name");
        JSONArray ports = (JSONArray) container.get("ports");
        for (Object p : ports) {
          JSONObject port = (JSONObject) p;
          Long containerPort = (Long) port.get("containerPort");
          containerPortNameMap.put(containerPort, name);
        }
      }
      debugMessage("containerPortNameMap="+containerPortNameMap);
      
      for (Object item : containerStatuses) {
        JSONObject status = (JSONObject) item;
        String name = (String) status.get("name");
        String id = (String) status.get("containerID");
        containerNameIdMap.put(name, id);
      }
      debugMessage("containerNameIdMap="+containerNameIdMap);
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Caught exception processing Pod metadata", t);
    }
    
    Map<String,Object> containerMetadata = new HashMap<String, Object>();
    containerMetadata.put(POD_NAMESPACE, podProps.get("MY_POD_NAMESPACE"));
    containerMetadata.put(POD_NAME, podProps.get("MY_POD_NAME"));
    containerMetadata.put(POD_IP, podProps.get("MY_POD_IP"));
    if (includeDetails) {
      containerMetadata.put(CONTAINER_PORT_NAME_MAP, containerPortNameMap);
      containerMetadata.put(CONTAINER_NAME_ID_MAP, containerNameIdMap);
    }
    debugMessage("output="+containerMetadata);
    return containerMetadata;
  }
  
  static void debugMessage(String... msgParts) {
    if (logger.isLoggable(Level.INFO)){
      StringBuilder msgBuf = new StringBuilder();
      for (String part : msgParts) {
        msgBuf.append(part).append(" ");
      }
      logger.log(Level.INFO, msgBuf.toString());
    }
  }
  
  public static void main(String[] args) throws Exception {
    List<String> podList = Arrays.asList(args);
    for (String podKey : podList) {
      debugMessage("#################################");
      debugMessage("Getting pod info for: " + podKey);
      debugMessage("PodInfo: " + getPodMetadata(podKey, true));
      debugMessage("#################################");
    }
    debugMessage("Done.");
  }
  
}
