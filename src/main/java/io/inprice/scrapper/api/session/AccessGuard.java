package io.inprice.scrapper.api.session;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import io.inprice.scrapper.api.app.auth.AuthRepository;
import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Commons;
import io.inprice.scrapper.api.helpers.ControllerHelper;
import io.inprice.scrapper.api.helpers.SessionHelper;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.info.ForCookie;
import io.inprice.scrapper.api.utils.NumberUtils;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class AccessGuard implements AccessManager {

   private final AuthRepository authRepository = Beans.getSingleton(AuthRepository.class);

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
               if (permittedRoles.contains(token.getRole())) {

                  ServiceResponse res = authRepository.findByHash(token.getHash());
                  if (res.isOK()) {
                     isDone = true;
                     CurrentUser.set(res.getData());
                     handler.handle(ctx);
                  } else {
                    ControllerHelper.removeExpiredAuthCookie(ctx);
                  }
               } else {
                  isDone = true;
                  ctx.json(Commons.createResponse(ctx, Responses._401));
               }
            }
         }
         if (! isDone) {
            ctx.removeCookie(Consts.SESSION);
            ctx.json(Commons.createResponse(ctx, Responses._401));
         }
      } else {
         ctx.status(HttpStatus.BAD_REQUEST_400).result("Invalid session");
      }
   }

}
