package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.common.meta.UserType;

public class Consts {

    public static class Auth {
        public static final String SECRET_KEY = "-8'fq{>As@njcx.U*$=P]#Z5wY+";
        public static final long TOKEN_EXPIRATION_TIME = 15 * 60 * 1000L;

        public static final String AUTHORIZATION_HEADER = "Authorization";
        public static final String TOKEN_PREFIX = "Bearer";

        public static final String LOGIN_ENDPOINT = "/login";
        public static final String REGISTRATION_ENDPOINT = "/register";
        public static final String AUTH_ENDPOINT_PREFIX = "/auth";

        public static final String USER_ID = "userId";
        public static final String USER_TYPE = "userType";
        public static final String USER_FULL_NAME = "fullName";
        public static final String COMPANY_ID = "companyId";
        public static final String WORKSPACE_ID = "workspaceId";
    }

}
