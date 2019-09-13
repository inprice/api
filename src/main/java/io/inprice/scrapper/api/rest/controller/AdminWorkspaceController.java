package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.AdminWorkspaceService;
import io.inprice.scrapper.common.utils.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class AdminWorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(AdminWorkspaceController.class);

    private static final AdminWorkspaceService service = Beans.getSingleton(AdminWorkspaceService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Workspace.BASE, (req, res) -> {
            ServiceResponse serviceRes = service.insert(toModel(req.body()));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.Workspace.BASE, (req, res) -> {
            ServiceResponse serviceRes = service.update(toModel(req.body()));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Workspace.BASE + "/:id", (req, res) -> {
            ServiceResponse serviceRes = service.deleteById(NumberUtils.toLong(req.params(":id")));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Workspace.BASE + "/:id", (req, res) -> {
            ServiceResponse serviceRes = service.findById(NumberUtils.toLong(req.params(":id")));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Workspace.BASE + "s", (req, res) -> {
            ServiceResponse serviceRes = service.getList();
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private WorkspaceDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, WorkspaceDTO.class);
        } catch (Exception e) {
            log.error("UserId: {} -> Data conversion error for workspace. " + body, Context.getUserId());
        }
        return null;
    }

}
