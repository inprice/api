package io.inprice.scrapper.api.consts;

import org.eclipse.jetty.http.HttpStatus;

import io.inprice.scrapper.api.info.ServiceResponse;

public class Responses {

   public static final ServiceResponse OK = new ServiceResponse(0, "OK");
   public static final ServiceResponse _401 = new ServiceResponse(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
   public static final ServiceResponse _403 = new ServiceResponse(HttpStatus.FORBIDDEN_403, "Forbidden");

   public static class Invalid {
      private static final int BASE = 100;
      public static final ServiceResponse COMPANY = new ServiceResponse(BASE + 1, "Invalid company!");
      public static final ServiceResponse PLAN = new ServiceResponse(BASE + 3, "Invalid plan!");
      public static final ServiceResponse TICKET = new ServiceResponse(BASE + 4, "Invalid ticket!");
      public static final ServiceResponse INVITATION = new ServiceResponse(BASE + 5, "Invalid invitation!");
      
      public static final ServiceResponse USER = new ServiceResponse(BASE + 10, "Invalid user!");
      public static final ServiceResponse EMAIL = new ServiceResponse(BASE + 11, "Invalid email!");
      public static final ServiceResponse PASSWORD = new ServiceResponse(BASE + 12, "Wrong password!");
      public static final ServiceResponse EMAIL_OR_PASSWORD = new ServiceResponse(BASE + 13,
      "Invalid email or password!");
      public static final ServiceResponse NAME = new ServiceResponse(BASE + 14, "Invalid name!");
      
      public static final ServiceResponse PRODUCT = new ServiceResponse(BASE + 20, "Invalid product!");
      public static final ServiceResponse LINK = new ServiceResponse(BASE + 21, "Invalid link!");
      
      public static final ServiceResponse TOKEN = new ServiceResponse(BASE + 30, "Invalid token!");
      public static final ServiceResponse EMPTY_FILE = new ServiceResponse(BASE + 40, "Empty file!");

      public static final ServiceResponse DATA = new ServiceResponse(BASE + 99, "Invalid data!");
   }

   public static class ServerProblem {
      private static final int BASE = 300;
      public static final ServiceResponse EXCEPTION = new ServiceResponse(BASE + 1, "Server error!");
      public static final ServiceResponse FAILED = new ServiceResponse(BASE + 2, "Operation failed!");
      public static final ServiceResponse LIMIT_PROBLEM = new ServiceResponse(BASE + 3, "Server limit problem!");
   }

   public static class Missing {
      private static final int BASE = 400;
      public static final ServiceResponse AUTHORIZATION_HEADER = new ServiceResponse(BASE + 1,
            "Authorization header is missing!");
   }

   public static class PermissionProblem {
      private static final int BASE = 500;
      public static final ServiceResponse UNAUTHORIZED = new ServiceResponse(BASE + 1, "Unauthrozied!");
      public static final ServiceResponse ADMIN_ONLY = new ServiceResponse(BASE + 2,
            "This operation can be done by an admin!");
      public static final ServiceResponse DONT_HAVE_A_PLAN = new ServiceResponse(BASE + 3,
            "You need to buy a new plan!");
      public static final ServiceResponse NO_COMPANY = new ServiceResponse(BASE + 5,
            "You have no active company! Please either create a new one or participate in an existing!");
   }

   public static class DataProblem {
      private static final int BASE = 600;
      public static final ServiceResponse DB_PROBLEM = new ServiceResponse(BASE + 1, "Database error!");
      public static final ServiceResponse REDIS_PROBLEM = new ServiceResponse(BASE + 2, "Redis error!");
      public static final ServiceResponse NOT_SUITABLE = new ServiceResponse(BASE + 10, "Not suitable!");
      public static final ServiceResponse ALREADY_EXISTS = new ServiceResponse(BASE + 11, "Already exists!");
      public static final ServiceResponse INTEGRITY_PROBLEM = new ServiceResponse(BASE + 12, "Integrity problem!");
      public static final ServiceResponse DUPLICATE = new ServiceResponse(BASE + 13, "Duplicate error!");

      public static final ServiceResponse FORM_VALIDATION = new ServiceResponse(BASE + 20, "Validation error!");
   }

   public static class Illegal {
      private static final int BASE = 700;
      public static final ServiceResponse TOO_MUCH_REQUEST = new ServiceResponse(BASE + 1,
            "You are trying too much, please wait some time!");
      public static final ServiceResponse TIMED_OUT_FORGOT_PASSWORD = new ServiceResponse(BASE + 2,
            "Your session seems to expire, please send us a new forgot password request again!");
   }

   public static class Already {
      private static final int BASE = 800;
      public static final ServiceResponse LOGGED_OUT = new ServiceResponse(BASE + 1, "Seems that you are already logged out!");

      public static class Defined {
         public static final ServiceResponse COMPANY = new ServiceResponse(BASE + 10, "Seems that you have already registered this company!");
      }
   }

   public static class Upload {
      private static final int BASE = 900;
      public static final ServiceResponse EMPTY = new ServiceResponse(BASE + 1, "File is empty!");
      public static final ServiceResponse MUST_BE_CSV = new ServiceResponse(BASE + 2, "Please upload a CSV file!");
      public static final ServiceResponse MUST_BE_TXT = new ServiceResponse(BASE + 3, "Please upload a text file!");
   }

   public static class NotFound {
      private static final int BASE = 404;

      public static final ServiceResponse SEARCH_NOT_FOUND = new ServiceResponse("Nothing found!"); // not an error!

      public static final ServiceResponse COMPANY = new ServiceResponse(BASE, "Company not found!");
      public static final ServiceResponse PLAN = new ServiceResponse(BASE, "Plan not found!");
      public static final ServiceResponse TICKET = new ServiceResponse(BASE, "Ticket not found!");

      public static final ServiceResponse USER = new ServiceResponse(BASE, "User not found!");
      public static final ServiceResponse EMAIL = new ServiceResponse(BASE, "Email not found!");
      public static final ServiceResponse INVITATION = new ServiceResponse(BASE, "Member not found!");

      public static final ServiceResponse PRODUCT = new ServiceResponse(BASE, "Product not found!");
      public static final ServiceResponse LINK = new ServiceResponse(BASE, "Link not found!");
      public static final ServiceResponse IMPORT = new ServiceResponse(BASE, "Import not found!");

      public static final ServiceResponse HISTORY = new ServiceResponse(BASE, "History not found!");

      public static final ServiceResponse DATA = new ServiceResponse(BASE, "Nothing found!");
   }

   public static class NotActive {
      private static final int BASE = 505;

      public static final ServiceResponse INVITATION = new ServiceResponse(BASE, "Invitation is not active!");
   }

}
