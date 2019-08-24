package io.inprice.scrapper.api.info;

import org.eclipse.jetty.http.HttpStatus;

public class InstantResponses {

    public static final ServiceResponse OK = new ServiceResponse(HttpStatus.OK_200, "OK");

    public static ImportReport FILE_PROBLEM(String tag) {
        return new ImportReport(HttpStatus.BAD_REQUEST_400, tag + " file is incorrect!");
    }

    public static ServiceResponse PERMISSION_PROBLEM(String info) {
        return new ServiceResponse(HttpStatus.FORBIDDEN_403, "User has no permission to " + info);
    }

    public static ServiceResponse NOT_FOUND(String tag) {
        return new ServiceResponse<>(HttpStatus.NOT_FOUND_404, tag + " not found!");
    }

    public static ServiceResponse INVALID_DATA(String tag) {
        return new ServiceResponse(HttpStatus.NOT_ACCEPTABLE_406, "Invalid data for " + tag);
    }

    public static ServiceResponse WRONG_PARAMETER(String info) {
        return new ServiceResponse(HttpStatus.NOT_ACCEPTABLE_406, info);
    }

    public static ServiceResponse ALREADY_EXISTS(String info) {
        return new ServiceResponse(HttpStatus.CONFLICT_409, info + " already exists in this workspace!");
    }

    public static ServiceResponse CRUD_ERROR(String info) {
        return new ServiceResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, "Database operation failed. " + info);
    }

    public static ServiceResponse SERVER_ERROR(Exception e) {
        return new ServiceResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, e.getMessage());
    }

}
