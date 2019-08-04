package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.info.Claims;
import io.inprice.scrapper.common.meta.UserType;

public class Consts {

    public static final String API_KEY = "API-KEY";
    public static final String CSRF_TOKEN = "CSRF-TOKEN";

    //TODO: should be dynamic in request object coming from web in Application.java
    public static final Claims ADMIN_CLAIMS = new Claims(1,1,1, UserType.ADMIN);
    public static final Claims USER_CLAIMS = new Claims(1,1,2, UserType.USER);

}
