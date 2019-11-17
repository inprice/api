package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.service.UserService;
import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.utils.NumberUtils;

import static spark.Spark.put;

public class UserController {

    private static final UserService service = Beans.getSingleton(UserService.class);

    @Routing
    public void routes() {

        //update. a user can edit only his/her data
        put(Consts.Paths.User.BASE, (req, res) -> {
            UserDTO user = Commons.toUserModel(req);
            if (user != null && UserType.EDITOR.equals(Context.getAuthUser().getType())) user.setId(Context.getUserId());
            return Commons.createResponse(res, service.update(user));
        }, Global.gson::toJson);

        //update password. a user can edit only his/her password
        put(Consts.Paths.User.PASSWORD, (req, res) -> {
            UserDTO user = Commons.toUserModel(req);
            if (user != null
            && (UserType.READER.equals(Context.getAuthUser().getType())
                || UserType.EDITOR.equals(Context.getAuthUser().getType()))) user.setId(Context.getUserId());
            return Commons.createResponse(res, service.updatePassword(user));
        }, Global.gson::toJson);

        //set active workspace
        put(Consts.Paths.User.SET_WORKSPACE + "/:id", (req, res) -> {
            return Commons.createResponse(res, service.setActiveWorkspace(NumberUtils.toLong(req.params(":id")), res));
        }, Global.gson::toJson);

    }

}
