package io.inprice.scrapper.api.info;

import org.eclipse.jetty.http.HttpStatus;

public class Responses {

    public static final Response OK = new Response(HttpStatus.OK_200, "OK");
    public static final Response CRUD_ERROR = new Response(HttpStatus.INTERNAL_SERVER_ERROR_500, "Database operation failed, please retry again!");
    public static final Response SERVER_ERROR = new Response(HttpStatus.INTERNAL_SERVER_ERROR_500, "Something went wrong in server. We will take care of it asap.");

    public static Response NOT_FOUND(String tag) {
        return new Response(HttpStatus.NOT_FOUND_404, tag + " not found!");
    }

    public static Response INVALID_PARAM(String tag) {
        return new Response(HttpStatus.BAD_REQUEST_400, tag + " is invalid!");
    }

}
