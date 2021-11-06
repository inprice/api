package io.inprice.api.session;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.CookieHelper;
import io.inprice.api.helpers.SessionHelper;
import io.inprice.api.meta.ShadowRoles;
import io.inprice.api.session.info.ForCookie;
import io.inprice.api.session.info.ForRedis;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.User;
import io.inprice.common.utils.NumberHelper;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class AccessGuard implements AccessManager {

  private static final Logger logger = LoggerFactory.getLogger(AccessGuard.class);

  private final RedisClient redis = Beans.getSingleton(RedisClient.class);
  
  @Override
  public void manage(Handler handler, Context ctx, Set<Role> permittedRoles) throws Exception {
    if ("OPTIONS".equals(ctx.method()) || permittedRoles.size() == 0) {
      handler.handle(ctx);
      return;
    }

    // we are checking first if the user is super user or not!
    String normalToken= ctx.cookieMap().get(Consts.SESSION);
    String superToken = ctx.cookieMap().get(Consts.SUPER_SESSION);

    //super user?
    if (StringUtils.isNotBlank(superToken)) {
  		User user = SessionHelper.fromTokenForSuper(superToken);

  		if (user != null && user.isPrivileged()) {
  			
      	if (permittedRoles.contains(ShadowRoles.SUPER) || (permittedRoles.contains(ShadowRoles.SUPER_WITH_WORKSPACE) && user.getWsId() != null)) {
      		MDC.put("email", user.getEmail());

      		CurrentUser.set(user);
          handler.handle(ctx);
  			} else {
  				if (permittedRoles.contains(ShadowRoles.SUPER_WITH_WORKSPACE) && user.getWsId() == null) {
  					ctx.json(Responses.NotAllowed.NO_WORKSPACE);
  				} else {
  					ctx.json(Responses.PermissionProblem.WRONG_USER);
  				}
  			}

  		} else {
  			CookieHelper.removeSuperCookie(ctx);
  			ctx.removeCookie(Consts.SUPER_SESSION);
  			ctx.json(Responses._403);
  		}

  	//normal user?
    } else {
      Integer sessionNo = NumberHelper.toInteger(ctx.header(Consts.SESSION_NO));
      if (sessionNo != null && sessionNo > -1) {
        boolean isDone = false;
  
        if (StringUtils.isNotBlank(normalToken)) {
  
          List<ForCookie> sessionTokens = SessionHelper.fromTokenForUser(normalToken);
          if (sessionTokens != null && sessionTokens.size() > sessionNo) {
  
            ForCookie token = sessionTokens.get(sessionNo);
            ShadowRoles role = ShadowRoles.valueOf(token.getRole());
            
            if (permittedRoles.contains(role)) {
            	MDC.put("email", token.getEmail());
  
              ForRedis redisSes = findByHash(token.getHash());
              if (redisSes != null) {
                isDone = true;
                CurrentUser.set(redisSes, sessionNo);
                handler.handle(ctx);
              } else {
                CookieHelper.removeUserCookie(ctx);
              }
            } else {
              isDone = true;
              ctx.json(Responses._403);
            }
          }
        }
        if (!isDone) {
          ctx.removeCookie(Consts.SESSION);
          ctx.json(Responses._401);
        }
      } else {
      	ctx.json(Responses._403);
      }
    }
  }

  ForRedis findByHash(String hash) {
    ForRedis ses = redis.getSession(hash);

    if (ses != null) {
      long diffInMillies = Math.abs(System.currentTimeMillis() - ses.getAccessedAt().getTime());
      long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

      if (diff > 7 && redis.refreshSesion(ses.getHash())) {
        try (Handle handle = Database.getHandle()) {
          UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
          if (! userSessionDao.refreshAccessedAt(hash)) {
            logger.warn("Failed to refresh accessed date for {}", hash);
          }
        }
      }
    }

    return ses;
  }

}
