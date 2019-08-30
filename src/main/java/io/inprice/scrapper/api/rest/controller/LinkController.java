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
import org.apache.commons.validator.routines.LongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class LinkController {

    private static final Logger log = LoggerFactory.getLogger(LinkController.class);

    private static final LinkService linkService = Beans.getSingleton(LinkService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Link.BASE, (req, res) -> {
            ServiceResponse serviceRes = insert(req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Link.BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Link.BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Link.BASE + "s/:product_id", (req, res) -> {
            final Long productId = LongValidator.getInstance().validate(req.params(":product_id"));

            ServiceResponse serviceRes = getList(productId);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //change status to RENEWED
        put(Consts.Paths.Link.RENEW + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));
            final Long productId = LongValidator.getInstance().validate(req.queryParams("product_id"));

            ServiceResponse serviceRes = changeStatus(id, productId, Status.RENEWED);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //change status to PAUSED
        put(Consts.Paths.Link.PAUSE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));
            final Long productId = LongValidator.getInstance().validate(req.queryParams("product_id"));

            ServiceResponse serviceRes = changeStatus(id, productId, Status.PAUSED);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //change status to RESUMED
        put(Consts.Paths.Link.RESUME + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));
            final Long productId = LongValidator.getInstance().validate(req.queryParams("product_id"));

            ServiceResponse serviceRes = changeStatus(id, productId, Status.RESUMED);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse findById(Long id) {
        return linkService.findById(id);
    }

    private ServiceResponse getList(long productId) {
        return linkService.getList(productId);
    }

    private ServiceResponse deleteById(Long id) {
        if (Context.getAuthUser().getType().equals(UserType.READER)) return InstantResponses.PERMISSION_PROBLEM("delete a link!");
        return linkService.deleteById(id);
    }

    private ServiceResponse insert(String body) {
        if (Context.getAuthUser().getType().equals(UserType.READER)) return InstantResponses.PERMISSION_PROBLEM("insert a new link!");

        LinkDTO linkDTO = toModel(body);
        if (linkDTO != null) {
            return linkService.insert(linkDTO);
        }
        log.error("Invalid link data: " + body);
        return InstantResponses.INVALID_DATA("link!");
    }

    private ServiceResponse changeStatus(Long id, Long productId, Status status) {
        if (Context.getAuthUser().getType().equals(UserType.READER)) return InstantResponses.PERMISSION_PROBLEM("change link's status!");
        return linkService.changeStatus(id, productId, status);
    }

    private LinkDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, LinkDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for link, body: " + body);
        }

        return null;
    }

}
