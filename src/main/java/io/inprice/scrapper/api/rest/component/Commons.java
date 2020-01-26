package io.inprice.scrapper.api.rest.component;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import spark.Request;
import spark.Response;

public class Commons {

    private static final Logger log = LoggerFactory.getLogger(Commons.class);

    public static ServiceResponse createResponse(Response res, ServiceResponse serviceResponse) {
        res.status(HttpStatus.OK_200);
        return serviceResponse;
    }

    public static ServiceResponse createResponse(List<String> problems) {
        if (problems.size() > 0) {
            return new ServiceResponse(problems);
        } else {
            return Responses.OK;
        }
    }

    public static UserDTO toUserModel(Request req) {
        if (! StringUtils.isBlank(req.body())) {
            try {
                return Global.gson.fromJson(req.body(), UserDTO.class);
            } catch (Exception e) {
                log.error("UserId: {} -> Data conversion error for user. " + req.body(), Context.getUserId());
            }
        }
        return null;
    }

    public static WorkspaceDTO toWorkspaceModel(Request req) {
        if (! StringUtils.isBlank(req.body())) {
            try {
                return Global.gson.fromJson(req.body(), WorkspaceDTO.class);
            } catch (Exception e) {
                log.error("UserId: {} -> Data conversion error for workspace. " + req.body(), Context.getUserId());
            }
        }
        return null;
    }

    public static ProductDTO toProductModel(Request req) {
        if (! StringUtils.isBlank(req.body())) {
            try {
                return Global.gson.fromJson(req.body(), ProductDTO.class);
            } catch (Exception e) {
                log.error("UserId: {} -> Data conversion error for workspace. " + req.body(), Context.getUserId());
            }
        }
        return null;
    }

}
