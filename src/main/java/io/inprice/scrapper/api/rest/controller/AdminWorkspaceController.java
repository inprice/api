package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AdminWorkspaceService;
import org.apache.commons.validator.routines.LongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class AdminWorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(AdminWorkspaceController.class);

    private static final AdminWorkspaceService adminWorkspaceService = Beans.getSingleton(AdminWorkspaceService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Workspace.BASE, (req, res) -> {
            ServiceResponse serviceRes = upsert(req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.Workspace.BASE, (req, res) -> {
            ServiceResponse serviceRes = upsert(req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Workspace.BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Workspace.BASE + "/:id", (req, res) -> {
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Workspace.BASE + "s", (req, res) -> {
            ServiceResponse serviceRes = getList();
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse findById(Long id) {
        return adminWorkspaceService.findById(id);
    }

    private ServiceResponse getList() {
        return adminWorkspaceService.getList();
    }

    private ServiceResponse deleteById(Long id) {
        return adminWorkspaceService.deleteById(id);
    }

    private ServiceResponse upsert(String body, boolean insert) {
        WorkspaceDTO workspaceDTO = toModel(body);
        if (workspaceDTO != null) {
            if (insert)
                return adminWorkspaceService.insert(workspaceDTO);
            else
                return adminWorkspaceService.update(workspaceDTO);
        }
        log.error("Invalid workspace data: " + body);
        return InstantResponses.INVALID_DATA("workspace!");
    }

    private WorkspaceDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, WorkspaceDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for workspace, body: " + body);
        }

        return null;
    }

}
