package io.inprice.scrapper.api.framework;

import io.javalin.Javalin;

public interface Controller {

   void addRoutes(Javalin app);

}