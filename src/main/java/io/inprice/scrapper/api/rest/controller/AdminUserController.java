package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.service.AdminUserService;
import io.inprice.scrapper.common.utils.NumberUtils;

import static spark.Spark.*;

public class AdminUserController {

    private static final AdminUserService service = Beans.getSingleton(AdminUserService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.AdminUser.BASE, (req, res) -> {
            return Commons.createResponse(res, service.insert(Commons.toUserModel(req)));
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.AdminUser.BASE, (req, res) -> {
            return Commons.createResponse(res, service.update(Commons.toUserModel(req)));
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.AdminUser.BASE + "/:id", (req, res) -> {
            return Commons.createResponse(res, service.deleteById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.AdminUser.BASE + "/:id", (req, res) -> {
            return Commons.createResponse(res, service.findById(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.AdminUser.BASE + "s", (req, res) -> {
            return Commons.createResponse(res, service.getList());
        }, Global.gson::toJson);

        //toggle active status
        put(Consts.Paths.AdminUser.TOGGLE_STATUS + "/:id", (req, res) -> {
            return Commons.createResponse(res, service.toggleStatus(NumberUtils.toLong(req.params(":id"))));
        }, Global.gson::toJson);

        //update password
        put(Consts.Paths.AdminUser.PASSWORD, (req, res) -> {
            return Commons.createResponse(res, service.updatePassword(Commons.toUserModel(req)));
        }, Global.gson::toJson);

    }

}
