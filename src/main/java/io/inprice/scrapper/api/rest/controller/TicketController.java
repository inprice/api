package io.inprice.scrapper.api.rest.controller;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.dto.TicketDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.TicketService;
import io.inprice.scrapper.common.meta.TicketSource;
import io.inprice.scrapper.common.utils.NumberUtils;
import spark.Request;

public class TicketController {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final TicketService service = Beans.getSingleton(TicketService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Ticket.BASE, (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            return Commons.createResponse(res, service.insert(toModel(req)));
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.Ticket.BASE, (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            return Commons.createResponse(res, service.update(toModel(req)));
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Ticket.BASE + "/:id", (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            return Commons.createResponse(res, service.deleteById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Ticket.BASE + "/:id", (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            return Commons.createResponse(res, service.findById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Ticket.BASE + "s/:source/:id", (req, res) -> {
        	if (res.status() >= 400) return new ServiceResponse(res.status());
            TicketSource source;
            try {
                source = TicketSource.valueOf(req.params(":source").toUpperCase());
            } catch (Exception e) {
                return Commons.createResponse(res, Responses.DataProblem.NOT_SUITABLE);
            }
            Long id = NumberUtils.toLong(req.params(":id"));
            return Commons.createResponse(res, service.getList(source, id));
        }, Global.gson::toJson);

    }

    private TicketDTO toModel(Request req) {
        if (! StringUtils.isBlank(req.body())) {
            try {
                return Global.gson.fromJson(req.body(), TicketDTO.class);
            } catch (Exception e) {
                log.error("UserId: {} -> Data conversion error for ticket. " + req.body(), Context.getUserId());
            }
        }
        return null;
    }

}
