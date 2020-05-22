package io.inprice.scrapper.api.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ControllerHelper
 */
public class ControllerHelper {

  public static Map<String, String> editSearchMap(Map<String, List<String>> queryMap) {
    return editSearchMap(queryMap, "");
  }

  public static Map<String, String> editSearchMap(Map<String, List<String>> queryMap, String extraQueryFields) {
    Map<String, String> searchMap = new HashMap<>(SqlHelper.STANDARD_SEARCH_MAP);
    if (queryMap.size() > 0) {
      for (Map.Entry<String, List<String>> entry : queryMap.entrySet()) {
        if (searchMap.containsKey(entry.getKey()) || extraQueryFields.indexOf(entry.getKey()) > -1) {
          searchMap.put(entry.getKey(), entry.getValue().get(0));
        }
      }
    }
    return searchMap;
  }

}