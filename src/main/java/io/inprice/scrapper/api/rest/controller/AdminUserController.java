package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.service.AdminUserService;
import org.apache.commons.validator.routines.LongValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class AdminUserController {

    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);
    
    private static final String ROOT = "admin/user";
    private final AdminUserService service = Beans.getSingleton(AdminUserService.class);

    @Routing
    public void routes() {

        //insert
        post(ROOT, (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            ServiceResponse serviceRes = upsert(Consts.ADMIN_CLAIMS, req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(ROOT, (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            ServiceResponse serviceRes = upsert(Consts.ADMIN_CLAIMS, req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(ROOT + "/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            ServiceResponse serviceRes = deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(ROOT + "/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            ServiceResponse serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(ROOT + "s", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            ServiceResponse serviceRes = getList(Consts.ADMIN_CLAIMS.getCompanyId());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //toggle active status
        put(ROOT + "/toggle-status/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            ServiceResponse serviceRes = toggleStatus(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update password
        put(ROOT + "/password", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            ServiceResponse serviceRes = updatePassword(Consts.ADMIN_CLAIMS, req.body());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    ServiceResponse findById(Long id) {
        return service.findById(id);
    }

    ServiceResponse getList(long companyId) {
        return service.getList(companyId);
    }

    ServiceResponse deleteById(Long id) {
        return service.deleteById(id);
    }

    ServiceResponse toggleStatus(Long id) {
        return service.toggleStatus(id);
    }

    ServiceResponse upsert(AuthUser claims, String body, boolean insert) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            if (insert)
                return service.insert(claims, userDTO);
            else
                return service.update(claims, userDTO, true);
        }
        log.error("Invalid user data: " + body);
        return new ServiceResponse(HttpStatus.BAD_REQUEST_400, "Invalid data for user!");
    }

    ServiceResponse updatePassword(AuthUser claims, String body) {
        PasswordDTO passwordDTO = toModel(body);
        if (passwordDTO != null) {
            return service.updatePassword(claims, passwordDTO);
        }
        log.error("Invalid password data: " + body);
        return new ServiceResponse(HttpStatus.BAD_REQUEST_400, "Invalid data for password!");
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
