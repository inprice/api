package io.inprice.api.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.session.CurrentUser;
import io.javalin.Javalin;
import io.javalin.http.Context;

public abstract class AbstractController {

	private static final Logger log = LoggerFactory.getLogger(AbstractController.class);

	protected abstract void addRoutes(Javalin app);

	protected void logForInvalidData(Context ctx, Exception e) {
		log.error("Failed to handle request! User: {}, Account: {}, IP: {}, Path: {}", CurrentUser.getUserId(),
				CurrentUser.getAccountName(), ctx.ip(), ctx.path(), e);
		log.error(ctx.body());
	}

}