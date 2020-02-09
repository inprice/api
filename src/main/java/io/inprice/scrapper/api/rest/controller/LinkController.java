package io.inprice.scrapper.api.rest.controller;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.LinkService;
import io.inprice.scrapper.common.meta.Status;
import io.inprice.scrapper.common.utils.NumberUtils;

public class LinkController {

    private static final Logger log = LoggerFactory.getLogger(LinkController.class);
    private static final LinkService service = Beans.getSingleton(LinkService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Link.BASE, (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            return Commons.createResponse(res, service.insert(toModel(req.body())));
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Link.BASE + "/:id", (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            final Long id = NumberUtils.toLong(req.params(":id"));
            return Commons.createResponse(res, service.deleteById(id));
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Link.BASE + "/:id", (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            return Commons.createResponse(res, service.findById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Link.BASE + "s/:product_id", (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            return Commons.createResponse(res, service.getList(NumberUtils.toLong(req.params(":product_id"))));
        }, Global.gson::toJson);

        //change status to RENEWED
        put(Consts.Paths.Link.RENEW + "/:id", (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            final Long id = NumberUtils.toLong(req.params(":id"));
            final Long productId = NumberUtils.toLong(req.queryParams("product_id"));
            return Commons.createResponse(res, service.changeStatus(id, productId, Status.RENEWED));
        }, Global.gson::toJson);

        //change status to PAUSED
        put(Consts.Paths.Link.PAUSE + "/:id", (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            final Long id = NumberUtils.toLong(req.params(":id"));
            final Long productId = NumberUtils.toLong(req.queryParams("product_id"));
            return Commons.createResponse(res, service.changeStatus(id, productId, Status.PAUSED));
        }, Global.gson::toJson);

        //change status to RESUMED
        put(Consts.Paths.Link.RESUME + "/:id", (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            final Long id = NumberUtils.toLong(req.params(":id"));
            final Long productId = NumberUtils.toLong(req.queryParams("product_id"));
            return Commons.createResponse(res, service.changeStatus(id, productId, Status.RESUMED));
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
