package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.UserService;

import static spark.Spark.put;

public class UserController {

    private static final UserService service = Beans.getSingleton(UserService.class);

    @Routing
    public void routes() {

        //update. a user can edit only his/her data
        put(Consts.Paths.User.BASE, (req, res) -> {
            UserDTO user = Commons.toUserModel(req);
            if (user != null) user.setId(Context.getUserId());
            return Commons.createResponse(res, service.update(user));
        }, Global.gson::toJson);

        //update password. a user can edit only his/her password
        put(Consts.Paths.User.PASSWORD, (req, res) -> {
            UserDTO user = Commons.toUserModel(req);
            if (user != null) user.setId(Context.getUserId());
            return Commons.createResponse(res, service.updatePassword(user));
        }, Global.gson::toJson);

    }

}
