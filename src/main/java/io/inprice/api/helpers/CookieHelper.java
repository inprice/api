package io.inprice.api.helpers;

import javax.servlet.http.Cookie;

import io.inprice.api.consts.Consts;
import io.inprice.common.config.SysProps;
import io.inprice.common.meta.AppEnv;
import io.javalin.http.Context;

/**
 * ControllerHelper
 */
public class CookieHelper {

  public static Cookie createAuthCookie(String token) {
    Cookie cookie = new Cookie(Consts.SESSION, token);
    cookie.setHttpOnly(true);
    if (SysProps.APP_ENV().equals(AppEnv.PROD)) {
      //cookie.setDomain(".inprice.io");
      cookie.setSecure(true);
      cookie.setMaxAge(Integer.MAX_VALUE);
    } else { // for dev and test purposes
      cookie.setMaxAge(60 * 60 * 24); // for one day
    }
    return cookie;
  }

  public static void removeAuthCookie(Context ctx) {
    Cookie cookie = createAuthCookie(null);
    cookie.setMaxAge(0);
    ctx.cookie(cookie);
  }

}