package io.inprice.api.framework;

import io.javalin.Javalin;

public abstract class AbstractController {

	protected abstract void addRoutes(Javalin app);

}