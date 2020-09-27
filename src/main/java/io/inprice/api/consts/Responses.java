package io.inprice.api.consts;

import org.eclipse.jetty.http.HttpStatus;

import io.inprice.api.info.Response;

public class Responses {

  public static final Response OK = new Response(0, "OK");
  public static final Response BAD_REQUEST = new Response(HttpStatus.BAD_REQUEST_400, "Bad request!");
  public static final Response _401 = new Response(HttpStatus.UNAUTHORIZED_401,
      "Your session seems to be expired, please sign in again.");
  public static final Response _403 = new Response(HttpStatus.FORBIDDEN_403,
      "Your role is not suitable to do this operation.");

  public static class Invalid {
    private static final int BASE = 100;
    public static final Response COMPANY = new Response(BASE + 1, "Invalid company!");
    public static final Response PLAN = new Response(BASE + 3, "Invalid plan!");
    public static final Response TICKET = new Response(BASE + 4, "Invalid ticket!");
    public static final Response INVITATION = new Response(BASE + 5, "Invalid invitation!");

    public static final Response USER = new Response(BASE + 10, "Invalid user!");
    public static final Response EMAIL = new Response(BASE + 11, "Invalid email!");
    public static final Response PASSWORD = new Response(BASE + 12, "Wrong password!");
    public static final Response EMAIL_OR_PASSWORD = new Response(BASE + 13,
        "Invalid email or password!");
    public static final Response NAME = new Response(BASE + 14, "Invalid name!");

    public static final Response PRODUCT = new Response(BASE + 20, "Invalid product!");
    public static final Response LINK = new Response(BASE + 21, "Invalid competitor!");

    public static final Response TOKEN = new Response(BASE + 30, "Invalid token!");
    public static final Response EMPTY_FILE = new Response(BASE + 40, "Empty file!");

    public static final Response COUPON = new Response(BASE + 45, "Invalid coupon!");

    public static final Response DATA = new Response(BASE + 99, "Invalid data!");
  }

  public static class ServerProblem {
    private static final int BASE = 300;
    public static final Response EXCEPTION = new Response(BASE + 1, "Server error!");
    public static final Response FAILED = new Response(BASE + 2, "Operation failed!");
  }

  public static class Missing {
    private static final int BASE = 400;
    public static final Response AUTHORIZATION_HEADER = new Response(BASE + 1,
        "Authorization header is missing!");
  }

  public static class PermissionProblem {
    private static final int BASE = 500;
    public static final Response UNAUTHORIZED = new Response(BASE + 1, "Unauthrozied!");
    public static final Response ADMIN_ONLY = new Response(BASE + 2,
        "This operation can be done by an admin!");

    public static final Response DONT_HAVE_A_PLAN = new Response(BASE + 3, "You need to buy a new plan!");
    public static final Response NO_COMPANY = new Response(BASE + 5,
        "You have no active company! Please either create a new one or participate in an existing!");
    public static final Response PRODUCT_LIMIT_PROBLEM = new Response(BASE + 3, "Your products count is reached your plans limit! You need to pass a broader plan to proceed");
  }

  public static class DataProblem {
    private static final int BASE = 600;
    public static final Response DB_PROBLEM = new Response(BASE + 1, "Database error!");
    public static final Response REDIS_PROBLEM = new Response(BASE + 2, "Redis error!");
    public static final Response NOT_SUITABLE = new Response(BASE + 10, "Not suitable!");
    public static final Response ALREADY_EXISTS = new Response(BASE + 11, "Already exists!");
    public static final Response INTEGRITY_PROBLEM = new Response(BASE + 12, "Integrity problem!");
    public static final Response DUPLICATE = new Response(BASE + 13, "Duplicate error!");

    public static final Response FORM_VALIDATION = new Response(BASE + 20, "Validation error!");
  }

  public static class Illegal {
    private static final int BASE = 700;
    public static final Response TIMED_OUT_FORGOT_PASSWORD = new Response(BASE + 1,
        "Your session seems to expire, please send us a new forgot password request again!");
    public static final Response COUPON_ISSUED_FOR_ANOTHER_COMPANY = new Response(BASE + 2,
        "This coupon is issued for another company!");
  }

  public static class Already {
    private static final int BASE = 800;
    public static final Response LOGGED_OUT = new Response(BASE + 1,
        "Seems that you are already logged out!");
    public static final Response DELETED_MEMBER = new Response(BASE + 2,
        "This member is already deleted!");

    public static final Response USED_COUPON = new Response(BASE + 5,
        "This coupon seems already used!");

    public static final Response ACTIVE_SUBSCRIPTION = new Response(BASE + 6,
        "You have already an active subscription. You cannot use any coupon!");
    public static final Response PASSIVE_SUBSCRIPTION = new Response(BASE + 7,
        "This account has no active subscription at the moment!");

    public static final Response REQUESTED_EMAIL = new Response(BASE + 10,
        "This email is already requested, please wait some time to try again!");

    public static class Defined {
      public static final Response COMPANY = new Response(BASE + 20,
          "Seems that you have already registered this company!");
      public static final Response MEMBERSHIP = new Response(BASE + 21,
          "Seems that this user has an account, please sign in with your credentials and manage your memberships under user settings page!");
    }
  }

  public static class Upload {
    private static final int BASE = 900;
    public static final Response EMPTY = new Response(BASE + 1, "File is empty!");
    public static final Response MUST_BE_CSV = new Response(BASE + 2, "Please upload a CSV file!");
    public static final Response MUST_BE_TXT = new Response(BASE + 3, "Please upload a text file!");
  }

  public static class NotFound {
    private static final int BASE = 404;

    public static final Response SEARCH_NOT_FOUND = new Response("Nothing found!"); // not an error!

    public static final Response COMPANY = new Response(BASE, "Company not found!");
    public static final Response PLAN = new Response(BASE, "Plan not found!");
    public static final Response TICKET = new Response(BASE, "Ticket not found!");
    public static final Response COUPON = new Response(BASE, "Coupon not found!");

    public static final Response USER = new Response(BASE, "User not found!");
    public static final Response EMAIL = new Response(BASE, "Email not found!");
    public static final Response MEMBERSHIP = new Response(BASE, "Member not found!");
    public static final Response INVITATION = new Response(BASE, "An active invitation not found!");

    public static final Response PRODUCT = new Response(BASE, "Product not found!");
    public static final Response LINK = new Response(BASE, "Competitor not found!");
    public static final Response IMPORT = new Response(BASE, "Import not found!");

    public static final Response HISTORY = new Response(BASE, "History not found!");
    public static final Response TRANSACTION = new Response(BASE, "Transaction not found!");

    public static final Response DATA = new Response(BASE, "Nothing found!");
  }

  public static class NotActive {
    private static final int BASE = 505;

    public static final Response INVITATION = new Response(BASE, "Invitation is not active!");
  }

}
