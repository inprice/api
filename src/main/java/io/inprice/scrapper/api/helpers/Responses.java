package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.info.ServiceResponse;

public class Responses {

    public static final ServiceResponse OK = new ServiceResponse(0);

    public static class Invalid {
        private static final int BASE = 100;
        public static final ServiceResponse COMPANY = new ServiceResponse(BASE + 1);
        public static final ServiceResponse WORKSPACE = new ServiceResponse(BASE + 2);
        public static final ServiceResponse PLAN = new ServiceResponse(BASE + 3);
        public static final ServiceResponse TICKET = new ServiceResponse(BASE + 4);

        public static final ServiceResponse USER = new ServiceResponse(BASE + 10);
        public static final ServiceResponse EMAIL = new ServiceResponse(BASE + 11);
        public static final ServiceResponse PASSWORD = new ServiceResponse(BASE + 12);
        public static final ServiceResponse EMAIL_OR_PASSWORD = new ServiceResponse(BASE + 13);

        public static final ServiceResponse PRODUCT = new ServiceResponse(BASE + 20);
        public static final ServiceResponse LINK = new ServiceResponse(BASE + 21);

        public static final ServiceResponse TOKEN = new ServiceResponse(BASE + 30);
        public static final ServiceResponse EMPTY_FILE = new ServiceResponse(BASE + 40);
    }

    public static class NotFound {
        private static final int BASE = 200;
        public static final ServiceResponse COMPANY = new ServiceResponse(BASE + 1);
        public static final ServiceResponse WORKSPACE = new ServiceResponse(BASE + 2);
        public static final ServiceResponse PLAN = new ServiceResponse(BASE + 3);
        public static final ServiceResponse TICKET = new ServiceResponse(BASE + 4);

        public static final ServiceResponse USER = new ServiceResponse(BASE + 10);
        public static final ServiceResponse EMAIL = new ServiceResponse(BASE + 11);

        public static final ServiceResponse PRODUCT = new ServiceResponse(BASE + 20);
        public static final ServiceResponse LINK = new ServiceResponse(BASE + 21);
        public static final ServiceResponse IMPORT = new ServiceResponse(BASE + 22);
    }

    public static class ServerProblem {
        private static final int BASE = 300;
        public static final ServiceResponse EXCEPTION = new ServiceResponse(BASE + 1);
        public static final ServiceResponse FAILED = new ServiceResponse(BASE + 2);
        public static final ServiceResponse LIMIT_PROBLEM = new ServiceResponse(BASE + 3);
    }

    public static class Missing {
        private static final int BASE = 400;
        public static final ServiceResponse AUTHORIZATION_HEADER = new ServiceResponse(BASE + 1);
    }

    public static class PermissionProblem {
        private static final int BASE = 500;
        public static final ServiceResponse UNAUTHORIZED = new ServiceResponse(BASE + 1);
        public static final ServiceResponse DONT_HAVE_A_PLAN = new ServiceResponse(BASE + 2);
    }

    public static class DataProblem {
        private static final int BASE = 600;
        public static final ServiceResponse DB_PROBLEM = new ServiceResponse(BASE + 1);
        public static final ServiceResponse NOT_SUITABLE = new ServiceResponse(BASE + 2);
        public static final ServiceResponse ALREADY_EXISTS = new ServiceResponse(BASE + 3);
        public static final ServiceResponse INTEGRITY_PROBLEM = new ServiceResponse(BASE + 4);
        public static final ServiceResponse DUPLICATE = new ServiceResponse(BASE + 5);

        public static final ServiceResponse FORM_VALIDATION = new ServiceResponse(BASE + 20);
        public static final ServiceResponse MASTER_WS_CANNOT_BE_DELETED = new ServiceResponse(BASE + 50);
        public static final ServiceResponse WS_HAS_USERS = new ServiceResponse(BASE + 52);
    }

}
