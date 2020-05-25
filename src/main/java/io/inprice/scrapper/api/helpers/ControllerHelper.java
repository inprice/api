package io.inprice.scrapper.api.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import io.inprice.scrapper.api.consts.Consts;
import io.javalin.http.Context;

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

  /**
   * Removes authentication cookie from requesting client
   * Please note: never use javalin's removeCookie method as it doesn't provide a way to set 
   * some attrs like Secure and HttpOnly
   */
  public static void removeExpiredAuthCookie(Context ctx) {
    Cookie cookie = new Cookie(Consts.SESSION, null);
    cookie.setMaxAge(0);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    ctx.cookie(cookie);
  }

}