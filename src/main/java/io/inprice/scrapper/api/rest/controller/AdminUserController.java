package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AdminUserService;
import io.inprice.scrapper.common.utils.NumberUtils;

import static spark.Spark.*;

public class AdminUserController {

    private static final AdminUserService service = Beans.getSingleton(AdminUserService.class);

    @Routing
    public void routes() {

        //insert
        post(Consts.Paths.AdminUser.BASE, (req, res) -> {
            ServiceResponse serviceRes = service.insert(toModel(req.body()));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(Consts.Paths.AdminUser.BASE, (req, res) -> {
            ServiceResponse serviceRes = service.update(toModel(req.body()));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(Consts.Paths.AdminUser.BASE + "/:id", (req, res) -> {
            Long id = NumberUtils.toLong(req.params(":id"));
            ServiceResponse serviceRes = service.deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(Consts.Paths.AdminUser.BASE + "/:id", (req, res) -> {
            Long id = NumberUtils.toLong(req.params(":id"));
            ServiceResponse serviceRes = service.findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(Consts.Paths.AdminUser.BASE + "s", (req, res) -> {
            ServiceResponse serviceRes = service.getList();
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //toggle active status
        put(Consts.Paths.AdminUser.TOGGLE_STATUS + "/:id", (req, res) -> {
            Long id = NumberUtils.toLong(req.params(":id"));
            ServiceResponse serviceRes = service.toggleStatus(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update password
        put(Consts.Paths.AdminUser.PASSWORD, (req, res) -> {
            ServiceResponse serviceRes = service.updatePassword(toModel(req.body()));
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    private UserDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, UserDTO.class);
        } catch (Exception e) {
            //
        }
        return null;
    }

}
