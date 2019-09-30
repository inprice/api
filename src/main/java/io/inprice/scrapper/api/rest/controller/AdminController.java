package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.AdminService;

import static spark.Spark.put;

public class AdminController {

    private static final AdminService service = Beans.getSingleton(AdminService.class);

    @Routing
    public void routes() {

        //update admin info
        put(Consts.Paths.Admin.BASE, (req, res) -> {
            UserDTO user = Commons.toUserModel(req);
            if (user != null) user.setId(Context.getUserId());
            return Commons.createResponse(res, service.update(user));
        }, Global.gson::toJson);

        //update admin password
        put(Consts.Paths.Admin.PASSWORD, (req, res) -> {
            UserDTO user = Commons.toUserModel(req);
            if (user != null) user.setId(Context.getUserId());
            return Commons.createResponse(res, service.updatePassword(user));
        }, Global.gson::toJson);

    }

}
