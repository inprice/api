package io.inprice.scrapper.api.rest.user;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Response;
import org.apache.commons.validator.routines.LongValidator;

import static spark.Spark.*;

public class Router {

    private static final String ROOT = "user";

    private final Controller controller = Beans.getSingleton(Controller.class);

    @Routing
    public void routes() {
        get(ROOT + "/by-id/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = controller.findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        get(ROOT + "/by-email/:email", (req, res) -> {
            Response serviceRes = controller.findByEmail(req.params(":email"));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(ROOT, (req, res) -> {
            Response serviceRes = controller.upsert(req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        put(ROOT, (req, res) -> {
            Response serviceRes = controller.upsert(req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);
    }

}
