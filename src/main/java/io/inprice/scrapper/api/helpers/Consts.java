package io.inprice.scrapper.api.helpers;

public class Consts {

    public static class Auth {
        public static final String APP_SECRET_KEY = "-8'fq{>As@njcx.U*$=P]#Z5wY+";
        static final String DATA_SECRET_KEY = "gFn+f3Ksa@YJWEq%8SeaM%MK^e";

        public static final String AUTHORIZATION_HEADER = "Authorization";
        public static final String WORKSPACE_HEADER = "workspace";
        public static final String TOKEN_PREFIX = "Bearer ";
        public static final String PAYLOAD = "payload";
    }

    public static class Paths {

        public static class Auth {
            public static final String LOGIN = "/login";
            public static final String REFRESH_TOKEN = "/refresh-token";
            public static final String RESET_PASSWORD = "/reset-password";
            public static final String FORGOT_PASSWORD = "/forgot-password";
            public static final String LOGOUT = "/logout";
        }

        public static final String ADMIN_BASE = "/admin";

        public static class Admin {
            public static final String BASE = ADMIN_BASE;
            public static final String PASSWORD = BASE + "/password";
        }

        public static class AdminUser {
            public static final String BASE = ADMIN_BASE + "/user";
            public static final String PASSWORD = BASE + "/password";
            public static final String TOGGLE_STATUS = BASE + "/toggle";
        }

        public static class Company {
            public static final String BASE = "/company";
            public static final String REGISTER = BASE + "/register";
        }

        public static class Workspace {
            public static final String BASE = ADMIN_BASE + "/workspace";
        }

        public static class User {
            public static final String BASE = "/user";
            public static final String PASSWORD = BASE + "/password";
        }

        public static class Product {
            public static final String BASE = "/product";
            public static final String TOGGLE_STATUS = BASE + "/toggle";

            public static final String IMPORT_BASE = BASE + "/import";
            public static final String IMPORT_CSV = IMPORT_BASE + "/csv";
            public static final String IMPORT_EBAY_SKU_LIST = IMPORT_BASE + "/ebay";
            public static final String IMPORT_AMAZON_ASIN_LIST = IMPORT_BASE + "/amazon";
        }

        public static class Link {
            public static final String BASE = "/link";
            public static final String RENEW = BASE + "/renew";
            public static final String PAUSE = BASE + "/pause";
            public static final String RESUME = BASE + "/resume";
        }

    }

}
