package io.inprice.api.helpers;

import javax.servlet.http.Cookie;

import io.inprice.api.config.Props;
import io.inprice.api.consts.Consts;
import io.javalin.http.Context;

/**
 * ControllerHelper
 */
public class CookieHelper {
	
	public static Cookie createUserCookie(String token) {
		Cookie cookie = new Cookie(Consts.SESSION, token);
		cookie.setHttpOnly(true);
		if (Props.getConfig().APP.ENV.equals(Consts.Env.PROD)) {
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
    if (Props.getConfig().APP.ENV.equals(Consts.Env.PROD)) {
    	cookie.setSecure(true);
    }
    cookie.setMaxAge(Props.getConfig().TTLS.COOKIE);
    return cookie;
  }

  public static void removeSuperCookie(Context ctx) {
    Cookie cookie = createSuperCookie(null);
    cookie.setMaxAge(0);
    ctx.cookie(cookie);
  }

}