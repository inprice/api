package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AdminWorkspaceService;
import io.inprice.scrapper.api.rest.service.AuthService;
import org.apache.commons.validator.routines.LongValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class AdminWorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(AdminWorkspaceController.class);

    private final AdminWorkspaceService adminWorkspaceService = Beans.getSingleton(AdminWorkspaceService.class);
    private final AuthService authService = Beans.getSingleton(AuthService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Workspace.BASE, (req, res) -> {
            final AuthUser authUser = authService.getAuthUser(req);

            ServiceResponse serviceRes = upsert(authUser, req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.Workspace.BASE, (req, res) -> {
            final AuthUser authUser = authService.getAuthUser(req);

            ServiceResponse serviceRes = upsert(authUser, req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Workspace.BASE + "/:id", (req, res) -> {
            final AuthUser authUser = authService.getAuthUser(req);
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = deleteById(authUser, id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Workspace.BASE + "/:id", (req, res) -> {
            final AuthUser authUser = authService.getAuthUser(req);
            final Long id = LongValidator.getInstance().validate(req.params(":id"));

            ServiceResponse serviceRes = findById(authUser, id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Workspace.BASE + "s", (req, res) -> {
            final AuthUser authUser = authService.getAuthUser(req);

            ServiceResponse serviceRes = getList(authUser);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private ServiceResponse findById(AuthUser authUser, Long id) {
        return adminWorkspaceService.findById(authUser, id);
    }

    private ServiceResponse getList(AuthUser authUser) {
        return adminWorkspaceService.getList(authUser);
    }

    private ServiceResponse deleteById(AuthUser authUser, Long id) {
        return adminWorkspaceService.deleteById(authUser, id);
    }

    private ServiceResponse upsert(AuthUser authUser, String body, boolean insert) {
        WorkspaceDTO workspaceDTO = toModel(body);
        if (workspaceDTO != null) {
            if (insert)
                return adminWorkspaceService.insert(authUser, workspaceDTO);
            else
                return adminWorkspaceService.update(authUser, workspaceDTO);
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
