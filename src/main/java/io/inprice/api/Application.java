package io.inprice.api;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Global;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.framework.ConfigScanner;
import io.inprice.api.framework.HandlerInterruptException;
import io.inprice.api.info.Response;
import io.inprice.api.session.AccessGuard;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.JsonConverter;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.models.AccessLog;
import io.javalin.Javalin;
import io.javalin.core.util.Header;
import io.javalin.core.util.RouteOverviewPlugin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.openapi.annotations.ContentType;

public class Application {

  private static final Logger log = LoggerFactory.getLogger(Application.class);

  private static Javalin app;
  private static RedisClient redis;
  
  public static void main(String[] args) {
    new Thread(() -> {
      log.info("APPLICATION IS STARTING...");

      createServer();
      ConfigScanner.scanControllers(app);
      
      log.info("APPLICATION STARTED.");
      Global.isApplicationRunning = true;

      redis = Beans.getSingleton(RedisClient.class);
      
    }, "app-starter").start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {

      Global.isApplicationRunning = false;
      log.info("APPLICATION IS TERMINATING...");

      log.info(" - Web server is shutting down...");
      app.stop();

      log.info(" - Redis connection is closing...");
      redis.shutdown();

      log.info(" - DB connection is closing...");
      Database.shutdown();

      log.info("ALL SERVICES IS DONE.");

    }, "shutdown-hook"));
  }

  private static void createServer() {
    app = Javalin.create((config) -> {
      config.defaultContentType = ContentType.JSON;
      config.enableCorsForAllOrigins();
      config.logIfServerNotStarted = true;
      config.showJavalinBanner = false;
      config.requestCacheSize = 8192L;

      if (SysProps.APP_ENV.equals(AppEnv.DEV)) {
        config.registerPlugin(new RouteOverviewPlugin("/routes"));
      }

      config.accessManager(new AccessGuard());

      JavalinJackson.configure(JsonConverter.mapper);
    }).start(Props.APP_PORT);

    app.before(ctx -> {
      if (ctx.method() == "OPTIONS") {
        ctx.header(Header.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
      } else {
      	ctx.sessionAttribute("started", System.currentTimeMillis());
    		ctx.sessionAttribute("body", ctx.body());
      }
    });

    app.after(ctx -> {
    	if (ctx.method() != "OPTIONS") {
      	logAccess(ctx, null);
    	}
    });

    app.exception(HandlerInterruptException.class, (e, ctx) -> {
    	logAccess(ctx, e);
      ctx.json(new Response(e.getStatus(), e.getMessage()));
    });

    app.exception(NotFoundResponse.class, (e, ctx) -> ctx.json(Responses.PAGE_NOT_FOUND));
    app.exception(BadRequestResponse.class, (e, ctx) -> ctx.json(Responses.REQUEST_BODY_INVALID));
  }

  private static void logAccess(Context ctx, HandlerInterruptException e) {
  	if (ctx.method() != "OPTIONS") {

  		int elapsed = 0;
  		Long started = ctx.sessionAttribute("started");
    	if (started != null) {
    		elapsed = (int)(System.currentTimeMillis()-started);
    	}
    	
    	boolean isSlow = (elapsed > Props.SERVICE_EXECUTION_THRESHOLD);

    	boolean
    		beLogged =
  				isSlow
    			|| ctx.res.getStatus() != 200
    			|| (ctx.method() != "GET" && !ctx.path().matches("(?i).*search.*|(?i).*\"term\":.*"));

  		if (beLogged) {
    		AccessLog userLog = new AccessLog();
    		if (CurrentUser.hasSession()) {
      		userLog.setUserId(CurrentUser.getUserId());
      		userLog.setUserEmail(CurrentUser.getEmail());
      		userLog.setUserRole(CurrentUser.getRole().name());
      		userLog.setAccountId(CurrentUser.getAccountId());
      		userLog.setAccountName(CurrentUser.getAccountName());
    		}
    		userLog.setIp(ctx.ip());
    		userLog.setAgent(ctx.userAgent());
    		userLog.setMethod(ctx.method());
    		userLog.setElapsed(elapsed);
    		userLog.setSlow(isSlow);
    		userLog.setPath(ctx.path());

    		if (StringUtils.isNotBlank(ctx.req.getQueryString())) {
    			userLog.setPathExt("?" + ctx.req.getQueryString());
    		}
    		
    		if (e != null) {
    			userLog.setStatus(e.getStatus());
    			userLog.setResBody(e.getMessage());
    		} else {
    			String body = (StringUtils.isNotBlank(ctx.resultString()) ? ctx.resultString().trim() : null);

    			userLog.setStatus(ctx.res.getStatus());
    			userLog.setResBody(body);
    			
    			if (body != null) {
    				if (body.charAt(0) == '{' || body.charAt(0) == '[') { //meaning that it is a json string!
        			Response res = JsonConverter.fromJson(body, Response.class);
        			userLog.setStatus(res.getStatus());
        			if (res.getStatus() != 200) {
        				userLog.setResBody(res.getReason());
        			}
    				}
    			}
    		}

      	String reqBody = ctx.sessionAttribute("body");
      	if (StringUtils.isNotBlank(reqBody)) {
      		userLog.setReqBody(reqBody.replaceAll("(?:ssword).*\"", "ssword\":\"***\""));
      	}

    		redis.addUserLog(userLog);
    	}
  	}
  	CurrentUser.cleanup();
  }

}
