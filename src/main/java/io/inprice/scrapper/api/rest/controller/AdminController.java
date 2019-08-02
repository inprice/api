package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
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
        get(ROOT + "/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = findById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        get(ROOT + "/all", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = getAll(Consts.claims.getCompanyId());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        post(ROOT, (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = upsert(Consts.claims.getCompanyId(), req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        put(ROOT, (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = upsert(Consts.claims.getCompanyId(), req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        put(ROOT + "/toggle-status/:id", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = toggleStatus(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        delete(ROOT + "/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    Response findById(Long id) {
        return service.findById(id);
    }

    Response getAll(long companyId) {
        return service.getAll(companyId);
    }

    Response deleteById(Long id) {
        return service.deleteById(id);
    }

    Response toggleStatus(Long id) {
        return service.toggleStatus(id);
    }

    Response upsert(long companyId, String body, boolean insert) {
        UserDTO userDTO = toModel(body);
        if (userDTO != null) {
            userDTO.setCompanyId(companyId); //
            if (insert)
                return service.insert(userDTO);
            else
                return service.update(userDTO);
        }
        log.error("Invalid user data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for user!");
    }

    private UserDTO toModel(String body) {
        try {
            return Global.gson.fromJson(body, UserDTO.class);
        } catch (Exception e) {
            log.error("Data conversion error for user!", e);
        }

        return null;
    }

}
