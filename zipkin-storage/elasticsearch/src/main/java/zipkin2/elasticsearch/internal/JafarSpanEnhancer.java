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
package zipkin2.elasticsearch.internal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.net.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class JafarSpanEnhancer {
  
  public static final String POD_IP = "podIP";
  public static final String POD_NAME = "podName";
  public static final String POD_NAMESPACE = "podNamespace";
  public static final String CONTAINER_NAME_ID_MAP = "containerNameIdMap";
  public static final String CONTAINER_PORT_NAME_MAP = "containerPortNameMap";

  // TODO: use WeakReference values and clean up the map periodically 
  static private Map<String,Map<String,Object>> podInfoCache = new ConcurrentHashMap<String, Map<String,Object>>();
  
  /**
   * This is strictly prototype; we'd likely have to listen for or query for pods coming and
   * going in order to prevent the cached Map from growing out of bounds over time.
   * 
   * @param podKey
   * @param includeDetails
   * @return
   */
  public static Map<String, Object> getPodMetadata(final String podKey, boolean includeDetails) {
    Map<String, Object> podInfoMap = null;
    podInfoMap = podInfoCache.get(podKey);
    if (podInfoMap == null || podInfoMap.isEmpty()) {
      System.out.println("+++++++++ Cache MISS for podKey: " + podKey);
      String[] podKeyList = podKey.split(":");
      if (podKeyList.length == 3) {
        String nodeName = podKeyList[0];
        String podName = podKeyList[1];
        String podIP = podKeyList[2];
        System.out.println("+++++++++ Pod name: " + podName + ", node: " + nodeName + ", pod IP: " + podIP);
        StringBuilder baseURL = new StringBuilder().append("http://").append(podIP).append(":9411/podinfo");
        podInfoMap = buildcontainerMetadata(baseURL.toString(), includeDetails);
        podInfoCache.put(podKey, podInfoMap);
      } else {
        System.out.println("---- ERROR, invalid pod key: " + podKey);
      }
    } else {
      System.out.println("+++++ Cache HIT for podKey " + podKey + ": " + podInfoMap);
    }
    return podInfoMap;
  }
  
  public static String readPodInfo(String baseURL) throws IOException {
    String podInfoURL = new StringBuilder(baseURL.toString()).append("/container-metadata.json").toString();
    URL podURL = new java.net.URL(podInfoURL);
    URLConnection conn = podURL.openConnection();
    BufferedReader in = new BufferedReader(new InputStreamReader(
                                conn.getInputStream()));
    StringBuilder builder = new StringBuilder();
    try {
      String inputLine;
      while ((inputLine = in.readLine()) != null)
        builder.append(inputLine).append("\n");
    } finally {
      in.close();
    }
    return builder.toString();   
  }
  
  private static Properties readPodProperties(String baseURL) throws IOException {
    String podPropsURL = new StringBuilder(baseURL.toString()).append("/pod-metadata.properties").toString();
    URL podURL = new java.net.URL(podPropsURL);
    URLConnection conn = podURL.openConnection();
    BufferedReader in = new BufferedReader(new InputStreamReader(
                                conn.getInputStream()));
    StringBuilder builder = new StringBuilder();
    try {
      String inputLine;
      while ((inputLine = in.readLine()) != null)
        builder.append(inputLine).append("\n");
    } finally {
      in.close();
    }
    Properties p = new Properties();
    p.load(new StringReader(builder.toString()));
    return p;
  }

  private static Map<String,Object> buildcontainerMetadata(String baseURL, boolean includeDetails) {

    Map<Long,String> containerPortNameMap = new HashMap<Long, String>();
    Map<String,String> containerNameIdMap = new HashMap<String, String>();

    Properties podProps = new Properties();
    try {
      Object obj = null;
      if (baseURL == null) {
        Process proc = Runtime.getRuntime().exec("/jafar-lib/generateK8SMetadata.sh");
        proc.waitFor();
        String input = "/jafar-lib/container-metadata.json";   
        obj = new JSONParser().parse(new FileReader(input));    
      } else {
        System.out.println("+++++++++ Reading pod info from " + baseURL);
        String jsonMap = readPodInfo(baseURL);
        obj = new JSONParser().parse(new StringReader(jsonMap));    
        
        podProps.putAll(readPodProperties(baseURL));
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
      System.out.println("containerPortNameMap="+containerPortNameMap);
      
      for (Object item : containerStatuses) {
        JSONObject status = (JSONObject) item;
        String name = (String) status.get("name");
        String id = (String) status.get("containerID");
        containerNameIdMap.put(name, id);
      }
      System.out.println("containerNameIdMap="+containerNameIdMap);
    } catch (Throwable t) {
      System.out.print("Caught exception processing Pod metadata: " + t.getMessage());
//      t.printStackTrace();
    }
    
    Map<String,Object> containerMetadata = new HashMap<String, Object>();
    containerMetadata.put(POD_NAMESPACE, podProps.get("MY_POD_NAMESPACE"));
    containerMetadata.put(POD_NAME, podProps.get("MY_POD_NAME"));
    containerMetadata.put(POD_IP, podProps.get("MY_POD_IP"));
    if (includeDetails) {
      containerMetadata.put(CONTAINER_PORT_NAME_MAP, containerPortNameMap);
      containerMetadata.put(CONTAINER_NAME_ID_MAP, containerNameIdMap);
    }
    System.out.println("output="+containerMetadata);
    return containerMetadata;
  }
  
  public static void main(String[] args) throws Exception {
    List<String> podList = Arrays.asList(args);
    for (String podKey : podList) {
      System.out.println("#################################");
      System.out.println("Getting pod info for: " + podKey);
      System.out.println("PodInfo: " + getPodMetadata(podKey, true));
      System.out.println("#################################");
    }
    System.out.println("Done.");
  }
  
}
