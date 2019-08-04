package io.inprice.scrapper.api.rest.controller;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.framework.Routing;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.info.Claims;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.rest.service.AdminWorkspaceService;
import org.apache.commons.validator.routines.LongValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

public class AdminWorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(AdminWorkspaceController.class);

    private static final String ROOT = "admin/workspace";
    private final AdminWorkspaceService service = Beans.getSingleton(AdminWorkspaceService.class);

    @Routing
    public void routes() {

        //insert
        post(ROOT, (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = upsert(Consts.ADMIN_CLAIMS, req.body(), true);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //update
        put(ROOT, (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = upsert(Consts.ADMIN_CLAIMS, req.body(), false);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //delete
        delete(ROOT + "/:id", (req, res) -> {
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = deleteById(id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //find
        get(ROOT + "/:id", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Long id = LongValidator.getInstance().validate(req.params(":id"));
            Response serviceRes = findById(Consts.ADMIN_CLAIMS, id);
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

        //list
        get(ROOT + "s", (req, res) -> {
            //todo: company id should be gotten from a real claim object coming from client
            Response serviceRes = getList(Consts.ADMIN_CLAIMS.getCompanyId());
            res.status(serviceRes.getStatus());
            return serviceRes;
        }, Global.gson::toJson);

    }

    Response findById(Claims claims, Long id) {
        return service.findById(claims, id);
    }

    Response getList(long companyId) {
        return service.getList(companyId);
    }

    Response deleteById(Long id) {
        return service.deleteById(id);
    }

    Response upsert(Claims claims, String body, boolean insert) {
        WorkspaceDTO workspaceDTO = toModel(body);
        if (workspaceDTO != null) {
            if (insert)
                return service.insert(claims, workspaceDTO);
            else
                return service.update(claims, workspaceDTO, true);
        }
        log.error("Invalid workspace data: " + body);
        return new Response(HttpStatus.BAD_REQUEST_400, "Invalid data for workspace!");
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
