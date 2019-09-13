package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.LinkService;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.utils.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class LinkController {

    private static final Logger log = LoggerFactory.getLogger(LinkController.class);

    private static final LinkService service = Beans.getSingleton(LinkService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Link.BASE, (req, res) -> {
            ServiceResponse serviceRes = service.insert(toModel(req.body()));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Link.BASE + "/:id", (req, res) -> {
            ServiceResponse serviceRes = service.deleteById(NumberUtils.toLong(req.params(":id")));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Link.BASE + "/:id", (req, res) -> {
            ServiceResponse serviceRes = service.findById(NumberUtils.toLong(req.params(":id")));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Link.BASE + "s/:product_id", (req, res) -> {
            ServiceResponse serviceRes = service.getList(NumberUtils.toLong(req.params(":product_id")));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //change status to RENEWED
        put(Consts.Paths.Link.RENEW + "/:id", (req, res) -> {
            final Long id = NumberUtils.toLong(req.params(":id"));
            final Long productId = NumberUtils.toLong(req.params(":product_id"));

            ServiceResponse serviceRes = service.changeStatus(id, productId, Status.RENEWED);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //change status to PAUSED
        put(Consts.Paths.Link.PAUSE + "/:id", (req, res) -> {
            final Long id = NumberUtils.toLong(req.params(":id"));
            final Long productId = NumberUtils.toLong(req.params(":product_id"));

            ServiceResponse serviceRes = service.changeStatus(id, productId, Status.PAUSED);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //change status to RESUMED
        put(Consts.Paths.Link.RESUME + "/:id", (req, res) -> {
            final Long id = NumberUtils.toLong(req.params(":id"));
            final Long productId = NumberUtils.toLong(req.params(":product_id"));

            ServiceResponse serviceRes = service.changeStatus(id, productId, Status.RESUMED);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private LinkDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, LinkDTO.class);
        } catch (Exception e) {
            log.error("UserId: {} -> Data conversion error for link. " + body, Context.getUserId());
        }

        return null;
    }

}
