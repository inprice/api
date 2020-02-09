package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.info.ServiceResponse;

public class Responses {

	public static final ServiceResponse OK = new ServiceResponse(0, "OK");
	public static final ServiceResponse _401 = new ServiceResponse(401, "Unauthorized!");
    public static final ServiceResponse _403 = new ServiceResponse(401, "Forbidden!");

    public static class Invalid {
        private static final int BASE = 100;
        public static final ServiceResponse COMPANY = new ServiceResponse(BASE + 1, "Invalid company!");
        public static final ServiceResponse WORKSPACE = new ServiceResponse(BASE + 2, "Invalid workspace!");
        public static final ServiceResponse PLAN = new ServiceResponse(BASE + 3, "Invalid plan!");
        public static final ServiceResponse TICKET = new ServiceResponse(BASE + 4, "Invalid ticket!");

        public static final ServiceResponse USER = new ServiceResponse(BASE + 10, "Invalid user!");
        public static final ServiceResponse EMAIL = new ServiceResponse(BASE + 11, "Invalid email!");
        public static final ServiceResponse PASSWORD = new ServiceResponse(BASE + 12, "Wrong password!");
        public static final ServiceResponse EMAIL_OR_PASSWORD = new ServiceResponse(BASE + 13, "Invalid email or password!");

        public static final ServiceResponse PRODUCT = new ServiceResponse(BASE + 20, "Invalid product!");
        public static final ServiceResponse LINK = new ServiceResponse(BASE + 21, "Invalid link!");

        public static final ServiceResponse TOKEN = new ServiceResponse(BASE + 30, "Invalid token!");
        public static final ServiceResponse EMPTY_FILE = new ServiceResponse(BASE + 40, "Empty file!");
    }

    public static class NotFound {
        private static final int BASE = 200;
        public static final ServiceResponse COMPANY = new ServiceResponse(BASE + 1, "Company not found!");
        public static final ServiceResponse WORKSPACE = new ServiceResponse(BASE + 2, "Workspace not found!");
        public static final ServiceResponse PLAN = new ServiceResponse(BASE + 3, "Plan not found!");
        public static final ServiceResponse TICKET = new ServiceResponse(BASE + 4, "Ticket not found!");

        public static final ServiceResponse USER = new ServiceResponse(BASE + 10, "User not found!");
        public static final ServiceResponse EMAIL = new ServiceResponse(BASE + 11, "Email not found!");

        public static final ServiceResponse PRODUCT = new ServiceResponse(BASE + 20, "Product not found!");
        public static final ServiceResponse LINK = new ServiceResponse(BASE + 21, "Link not found!");
        public static final ServiceResponse IMPORT = new ServiceResponse(BASE + 22, "Import not found!");
    }

    public static class ServerProblem {
        private static final int BASE = 300;
        public static final ServiceResponse EXCEPTION = new ServiceResponse(BASE + 1, "Server error!");
        public static final ServiceResponse FAILED = new ServiceResponse(BASE + 2, "Operation failed!");
        public static final ServiceResponse LIMIT_PROBLEM = new ServiceResponse(BASE + 3, "Server limit problem!");
    }

    public static class Missing {
        private static final int BASE = 400;
        public static final ServiceResponse AUTHORIZATION_HEADER = new ServiceResponse(BASE + 1, "Authorization header is missing!");
    }

    public static class PermissionProblem {
        private static final int BASE = 500;
        public static final ServiceResponse UNAUTHORIZED = new ServiceResponse(BASE + 1, "Unauthrozied!");
        public static final ServiceResponse DONT_HAVE_A_PLAN = new ServiceResponse(BASE + 2, "You need to buy a new plan!");
    }

    public static class DataProblem {
        private static final int BASE = 600;
        public static final ServiceResponse DB_PROBLEM = new ServiceResponse(BASE + 1, "Database error!");
        public static final ServiceResponse NOT_SUITABLE = new ServiceResponse(BASE + 2, "Not suitable!");
        public static final ServiceResponse ALREADY_EXISTS = new ServiceResponse(BASE + 3, "Already exists!");
        public static final ServiceResponse INTEGRITY_PROBLEM = new ServiceResponse(BASE + 4, "Integrity problem!");
        public static final ServiceResponse DUPLICATE = new ServiceResponse(BASE + 5, "Duplicate error!");

        public static final ServiceResponse FORM_VALIDATION = new ServiceResponse(BASE + 20, "Validation error!");
        public static final ServiceResponse MASTER_WS_CANNOT_BE_DELETED = new ServiceResponse(BASE + 50, "Master workspace cannot be deleted!");
        public static final ServiceResponse WS_HAS_USERS = new ServiceResponse(BASE + 52, "This workspace has users!");
    }

    public static class Illegal {
        private static final int BASE = 700;
        public static final ServiceResponse TOO_MUCH_REQUEST = new ServiceResponse(BASE + 1, "You are trying too much, please wait some time!");
    }

}
