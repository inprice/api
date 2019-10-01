package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.service.AdminWorkspaceService;
import io.inprice.scrapper.common.utils.NumberUtils;

import static spark.Spark.*;

public class AdminWorkspaceController {

    private static final AdminWorkspaceService service = Beans.getSingleton(AdminWorkspaceService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.Workspace.BASE, (req, res) -> {
            return Commons.createResponse(res, service.insert(Commons.toWorkspaceModel(req)));
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.Workspace.BASE, (req, res) -> {
            return Commons.createResponse(res, service.update(Commons.toWorkspaceModel(req)));
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.Workspace.BASE + "/:id", (req, res) -> {
            return Commons.createResponse(res, service.deleteById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.Workspace.BASE + "/:id", (req, res) -> {
            return Commons.createResponse(res, service.findById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.Workspace.BASE + "s", (req, res) -> {
            return Commons.createResponse(res, service.getList());
        }, Global.gson::toJson);

        //toggle active status
        put(Consts.Paths.Workspace.TOGGLE_STATUS + "/:id", (req, res) -> {
            return Commons.createResponse(res, service.toggleStatus(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

    }

}
