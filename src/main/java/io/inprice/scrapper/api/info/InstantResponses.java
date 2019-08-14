package io.inprice.scrapper.api.info;

import io.inprice.scrapper.common.models.Model;
import org.eclipse.jetty.http.HttpStatus;

public class InstantResponses {

    public static final ServiceResponse OK = new ServiceResponse(HttpStatus.OK_200, "OK");
    public static final ServiceResponse CRUD_ERROR = new ServiceResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, "Database operation failed, please retry again!");

    public static <T extends Model> ServiceResponse<T> NOT_FOUND(String tag) {
        return new ServiceResponse<>(HttpStatus.NOT_FOUND_404, tag + " not found!");
    }

    public static ServiceResponse PERMISSION_PROBLEM(String info) {
        return new ServiceResponse(HttpStatus.FORBIDDEN_403, "User has no permission to " + info);
    }

    public static ServiceResponse INVALID_DATA(String tag) {
        return new ServiceResponse(HttpStatus.NOT_ACCEPTABLE_406, "Invalid data for " + tag);
    }

    public static ServiceResponse SERVER_ERROR(Exception e) {
        return new ServiceResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
    }

}
