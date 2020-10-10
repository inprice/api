package io.inprice.api.session;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.Commons;
import io.inprice.api.helpers.CookieHelper;
import io.inprice.api.helpers.SessionHelper;
import io.inprice.api.meta.ShadowRoles;
import io.inprice.api.session.info.ForCookie;
import io.inprice.api.session.info.ForRedis;
import io.inprice.common.helpers.Database;
import io.inprice.common.utils.NumberUtils;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class AccessGuard implements AccessManager {

  private static final Logger log = LoggerFactory.getLogger(AccessGuard.class);

  @Override
  public void manage(Handler handler, Context ctx, Set<Role> permittedRoles) throws Exception {
    if ("OPTIONS".equals(ctx.method()) || permittedRoles.size() == 0) {
      handler.handle(ctx);
      return;
    }

    Integer sessionNo = NumberUtils.toInteger(ctx.header(Consts.SESSION_NO));
    if (sessionNo != null && sessionNo > -1) {
      boolean isDone = false;

      String tokenString = ctx.cookieMap().get(Consts.SESSION);
      if (StringUtils.isNotBlank(tokenString)) {

        List<ForCookie> sessionTokens = SessionHelper.fromToken(tokenString);
        if (sessionTokens != null && sessionTokens.size() > sessionNo) {

          ForCookie token = sessionTokens.get(sessionNo);
          ShadowRoles role = ShadowRoles.valueOf(token.getRole());
          if (permittedRoles.contains(role)) {

            ForRedis redisSes = findByHash(token.getHash());
            if (redisSes != null) {
              isDone = true;
              CurrentUser.set(redisSes);
              handler.handle(ctx);
            } else {
              CookieHelper.removeAuthCookie(ctx);
            }
          } else {
            isDone = true;
            ctx.json(Commons.createResponse(ctx, Responses._401));
          }
        }
      }
      if (!isDone) {
        ctx.removeCookie(Consts.SESSION);
        ctx.json(Commons.createResponse(ctx, Responses._401));
      }
    } else {
      ctx.status(HttpStatus.BAD_REQUEST_400).result("Invalid session!");
    }
  }

  ForRedis findByHash(String hash) {
    ForRedis ses = RedisClient.getSession(hash);

    if (ses != null) {
      long diffInMillies = Math.abs(System.currentTimeMillis() - ses.getAccessedAt().getTime());
      long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

      if (diff > 7 && RedisClient.refreshSesion(ses.getHash())) {
        try (Handle handle = Database.getHandle()) {
          UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
          if (! userSessionDao.refreshAccessedAt(hash)) {
            log.warn("Failed to refresh accessed date for {}", hash);
          }
        }
      }
    }

    return ses;
  }

}