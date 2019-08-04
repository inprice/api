package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Claims;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.rest.service.AdminService;
import org.apache.commons.validator.routines.LongValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService service = Beans.getSingleton(AdminService.class);

    private static final String ROOT = "admin";

    @Routing
    public void routes() {

        //update admin
        put(ROOT + "/update", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = updateAdmin(Consts.ADMIN_CLAIMS, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update admin password
        put(ROOT + "/update-password", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = updateAdminPassword(Consts.ADMIN_CLAIMS, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //insert a user
        post(ROOT + "/user", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = upsertUser(Consts.ADMIN_CLAIMS, req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update a user
        put(ROOT + "/user/update", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = upsertUser(Consts.ADMIN_CLAIMS, req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete a user
        delete(ROOT + "/user/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find a user by id
        get(ROOT + "/user/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //get user list
        get(ROOT + "/users", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = getList(Consts.ADMIN_CLAIMS.getCompanyId());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //toggle a user's active status
        put(ROOT + "/user/toggle-status/:id", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = toggleStatus(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    Response findById(Long id) {
        return service.findById(id);
    }

    Response getList(long companyId) {
        return service.getList(companyId);
    }

    Response deleteById(Long id) {
        return service.deleteById(id);
    }

    Response toggleStatus(Long id) {
        return service.toggleStatus(id);
    }

    Response upsertUser(Claims claims, String body, boolean insert) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            if (insert)
                return service.insert(claims, userDTO);
            else
                return service.update(claims, userDTO, true);
        }
        log.error("Invalid user data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for user!");
    }

    Response updateAdmin(Claims claims, String body) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            return service.update(claims, userDTO, false);
        }
        log.error("Invalid user data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for user!");
    }

    Response updateAdminPassword(Claims claims, String body) {
        PasswordDTO passwordDTO = toModel(body);
        if (passwordDTO != null) {
            return service.updatePassword(claims, passwordDTO);
        }
        log.error("Invalid password data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for password!");
    }

    private UserDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, UserDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for user, body: " + body);
        }

        return null;
    }

}
