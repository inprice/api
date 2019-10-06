package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.TicketDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.TicketService;
import io.inprice.scrapper.common.meta.TicketSource;
import io.inprice.scrapper.common.utils.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import static spark.Spark.*;

public class TicketController {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final TicketService service = Beans.getSingleton(TicketService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Ticket.BASE, (req, res) -> {
            return Commons.createResponse(res, service.insert(toModel(req)));
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.Ticket.BASE, (req, res) -> {
            return Commons.createResponse(res, service.update(toModel(req)));
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Ticket.BASE + "/:id", (req, res) -> {
            return Commons.createResponse(res, service.deleteById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Ticket.BASE + "/:id", (req, res) -> {
            return Commons.createResponse(res, service.findById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Ticket.BASE + "s/:source/:id", (req, res) -> {
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
