package io.inprice.api.helpers;

import javax.servlet.http.Cookie;

import io.inprice.api.consts.Consts;
import io.inprice.api.external.Props;
import io.inprice.common.config.SysProps;
import io.inprice.common.meta.AppEnv;
import io.javalin.http.Context;

/**
 * ControllerHelper
 */
public class CookieHelper {
	
	public static Cookie createUserCookie(String token) {
		Cookie cookie = new Cookie(Consts.SESSION, token);
		cookie.setHttpOnly(true);
		if (SysProps.APP_ENV().equals(AppEnv.PROD)) {
			cookie.setSecure(true);
		}
		cookie.setMaxAge(Integer.MAX_VALUE);
		return cookie;
	}

  public static void removeUserCookie(Context ctx) {
  	Cookie cookie = createUserCookie(null);
  	cookie.setMaxAge(0);
  	ctx.cookie(cookie);
  }

  public static Cookie createSuperCookie(String token) {
    Cookie cookie = new Cookie(Consts.SUPER_SESSION, token);
    cookie.setHttpOnly(true);
    if (SysProps.APP_ENV().equals(AppEnv.PROD)) {
    	cookie.setSecure(true);
    }
    cookie.setMaxAge(Props.TTL_NORMAL_COOKIES()); // one hour
    return cookie;
  }

  public static void removeSuperCookie(Context ctx) {
    Cookie cookie = createSuperCookie(null);
    cookie.setMaxAge(0);
    ctx.cookie(cookie);
  }

}