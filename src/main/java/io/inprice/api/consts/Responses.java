package io.inprice.api.consts;

import org.eclipse.jetty.http.HttpStatus;

import io.inprice.api.info.Response;

public class Responses {

  public static final Response OK = new Response(HttpStatus.OK_200, "OK");
  public static final Response BAD_REQUEST = new Response(HttpStatus.BAD_REQUEST_400, "Bad request!");
  public static final Response _401 = new Response(HttpStatus.UNAUTHORIZED_401, "No active session, please sign in!");
  public static final Response _403 = new Response(HttpStatus.FORBIDDEN_403, "Forbidden!");
  public static final Response BANNED_USER = new Response(HttpStatus.FORBIDDEN_403, "Banned user!");
  public static final Response REQUEST_BODY_INVALID = new Response(HttpStatus.BAD_REQUEST_400, "Request body is invalid!");
  public static final Response PAGE_NOT_FOUND = new Response(HttpStatus.NOT_FOUND_404, "Page not found!");
  public static final Response METHOD_NOT_ALLOWED = new Response(HttpStatus.METHOD_NOT_ALLOWED_405, "Method not allowed!");
  
  public static final Response EMPTY_REPORT = new Response(HttpStatus.BAD_REQUEST_400, "No data found to generate the report!");
  public static final Response REPORT_PROBLEM = new Response(HttpStatus.INTERNAL_SERVER_ERROR_500, "We are sorry, the report you wanted to generate encountered an error.");

  public static class Invalid {
    private static final int BASE = 100;
    public static final Response WORKSPACE = new Response(BASE + 1, "Invalid workspace!");
    public static final Response PLAN = new Response(BASE + 3, "Invalid plan!");
    public static final Response TICKET = new Response(BASE + 4, "Invalid ticket!");
    public static final Response COMMENT = new Response(BASE + 5, "Invalid comment!");
    public static final Response INVITATION = new Response(BASE + 6, "Invalid invitation!");
    public static final Response ANNOUNCE = new Response(BASE + 7, "Invalid announce!");
    public static final Response ALARM = new Response(BASE + 8, "Invalid alarm!");
    public static final Response ALARM_TOPIC = new Response(BASE + 9, "Invalid alarm topic!");

    public static final Response USER = new Response(BASE + 10, "Invalid user!");
    public static final Response EMAIL = new Response(BASE + 11, "Invalid email!");
    public static final Response PASSWORD = new Response(BASE + 12, "Wrong password!");
    public static final Response EMAIL_OR_PASSWORD = new Response(BASE + 13, "Invalid email or password!");
    public static final Response NAME = new Response(BASE + 14, "Invalid name!");

    public static final Response PRODUCT = new Response(BASE + 20, "Invalid product!");
    public static final Response LINK = new Response(BASE + 21, "Invalid link!");
    public static final Response TOKEN = new Response(BASE + 30, "Invalid token!");

    public static final Response VOUCHER = new Response(BASE + 45, "Invalid voucher!");
    
    public static final Response FILE_TYPE = new Response(BASE + 60, "Invalid file type!");
    public static final Response FILE_LENGTH_TOO_LARGE = new Response(BASE + 62, "File length is too large! Must be less than 1 mb.");

    public static final Response PRICE = new Response(BASE + 65, "Invalid price!");
        
    public static final Response CSV_COLUMN_COUNT = new Response(BASE + 90, "Column count mismatch!");
    public static final Response DATA = new Response(BASE + 99, "Invalid data!");
  }

  public static class ServerProblem {
    private static final int BASE = 300;
    public static final Response EXCEPTION = new Response(BASE + 1, "Server error! Please contact us via support@inprice.io");
    public static final Response FAILED = new Response(BASE + 2, "Operation failed! Please contact us via support@inprice.io");
    public static final Response CHECKOUT_PROBLEM = new Response(BASE + 3, "Checkout problem! Please inform us via support@inprice.io, if it fails again");
  }

  public static class Missing {
    private static final int BASE = 400;
    public static final Response AUTHORIZATION_HEADER = new Response(BASE + 1, "Authorization header is missing!");
  }

  public static class PermissionProblem {
    private static final int BASE = 500;
    public static final Response UNAUTHORIZED = new Response(BASE + 1, "Unauthrozied!");
    public static final Response ADMIN_ONLY = new Response(BASE + 2, "This operation can only be done by an admin!");
    public static final Response USER_LIMIT_PROBLEM = new Response(BASE + 6, "Your user count is reached your plans limit!");
    public static final Response PRODUCT_LIMIT_PROBLEM = new Response(BASE + 7, "Your product count is reached your plans limit. You need to subscribe to a broader plan!");
    public static final Response ALARM_LIMIT_PROBLEM = new Response(BASE + 8, "Your alarm count is reached your plans limit! You need to pass a broader plan to proceed");
    public static final Response BROADER_PLAN_NEEDED = new Response(BASE + 10, "You need to select a broader plan since your actual plan has more permission!");
    public static final Response WRONG_USER = new Response(BASE + 11, "You are not allowed to do this operation!");  
  }

  public static class DataProblem {
    private static final int BASE = 600;
    public static final Response DB_PROBLEM = new Response(BASE + 1, "Database error!");
    public static final Response REDIS_PROBLEM = new Response(BASE + 2, "Redis error!");
    public static final Response NOT_SUITABLE = new Response(BASE + 10, "Not suitable!");
    public static final Response ALREADY_EXISTS = new Response(BASE + 11, "Already exists!");
    public static final Response INTEGRITY_PROBLEM = new Response(BASE + 12, "Integrity problem!");
    public static final Response DUPLICATE = new Response(BASE + 13, "Duplicate error!");
    public static final Response SUBSCRIPTION_PROBLEM = new Response(BASE + 15, "Subscription service is not reachable right now! We are, sorry for this situation and, investigating.");
    public static final Response FORM_VALIDATION = new Response(BASE + 20, "Validation error!");
  }

  public static class Illegal {
    private static final int BASE = 700;
    public static final Response TIMED_OUT_FORGOT_PASSWORD = new Response(BASE + 1, "Your session seems to expire, please send us a new 'forgot password request' again!");
    public static final Response VOUCHER_ISSUED_FOR_ANOTHER_WORKSPACE = new Response(BASE + 2, "This voucher is issued for another workspace!");
    public static final Response INCOMPATIBLE_CONTENT = new Response(BASE + 3, "Incompatible content!");
    public static final Response NOT_SUITABLE_FOR_CANCELLATION = new Response(BASE + 10, "You don't have an active plan to cancel!");
    public static final Response NO_FREE_USE_RIGHT = new Response(BASE + 11, "You have no free use right!");
    public static final Response BANNED_USER = new Response(BASE + 16, "Banned user!");
  }

  public static class Already {
    private static final int BASE = 800;
    public static final Response LOGGED_OUT = new Response(BASE + 1, "Seems that you are already logged out!");
    public static final Response DELETED_MEMBER = new Response(BASE + 2, "This member is already deleted!");
    public static final Response PAUSED_MEMBER = new Response(BASE + 3, "This member is already paused!");

    public static final Response USED_VOUCHER = new Response(BASE + 4, "This voucher is already used!");
    public static final Response FREE_USE_USED = new Response(BASE + 5, "You have already used your free use!");

    public static final Response ACTIVE_SUBSCRIPTION = new Response(BASE + 7, "You already have an active subscription!");
    public static final Response PASSIVE_SUBSCRIPTION = new Response(BASE + 8, "This workspace has no active subscription at the moment!");
    public static final Response HAS_THE_SAME_PLAN = new Response(BASE + 10, "You have already this plan at the moment!");

    public static final Response REQUESTED_EMAIL = new Response(BASE + 14, "This email is already requested, please wait some time to try again!");
    public static final Response RESET_PASSWORD = new Response(BASE + 15, "Your password is already reset!");
    
    public static final Response BANNED_USER = new Response(BASE + 25, "User is already banned!");
    public static final Response NOT_BANNED_USER = new Response(BASE + 26, "User is not banned!");

    public static final Response BANNED_WORKSPACE = new Response(BASE + 30, "Workspace is already banned!");
    public static final Response NOT_BANNED_WORKSPACE = new Response(BASE + 31, "Workspace is not banned!");

    public static class Defined {
      public static final Response WORKSPACE = new Response(BASE + 70, "Seems that you have already registered this workspace!");
      public static final Response MEMBERSHIP = new Response(BASE + 71, "Seems that this user has an workspace, please sign in with your credentials and manage your members under user settings page!");
      public static final Response REGISTERED_USER = new Response(BASE + 72, "Already registered user! Signing up is an option for only newcomers! Please use 'Create Workspace' menu after login.");
      public static final Response PRODUCT = new Response(BASE + 75, "You already have a product having the same sku!");
      public static final Response BRAND = new Response(BASE + 76, "This brand has already been added!");
      public static final Response CATEGORY = new Response(BASE + 77, "This category has already been added!");

      public static final Response ALARM = new Response(BASE + 80, "You have already defined this alarm previously!");
      public static final Response SMART_PRICE = new Response(BASE + 81, "This formula has already been added!");
    }
  }

  public static class NotAllowed {
    private static final int BASE = 900;
    public static final Response LINK_LIMIT_EXCEEDED = new Response(BASE + 2, "You are allowed to upload up to 25 URLs at once!");
    public static final Response HAVE_NO_ACTIVE_PLAN = new Response(BASE + 3, "You don't have an active plan!");
    public static final Response UPDATE = new Response(BASE + 4, "You are not allowed to update this data!");
    
    public static final Response NO_ALARM_LIMIT = new Response(BASE + 10, "You have reached max alarm number of your plan!");
    public static final Response NO_PRODUCT_LIMIT = new Response(BASE + 11, "You have reached max product number of your plan!");
    public static final Response NO_USER_LIMIT = new Response(BASE + 12, "You have reached max user number of your plan!");
    
    public static final Response SUPER_USER = new Response(BASE + 13, "User is not suitable for this operation!");
    public static final Response CLOSED_TICKET = new Response(BASE + 14, "Ticket is closed!");
    public static final Response NO_WORKSPACE = new Response(BASE + 15, "You must bind to a workspace!");
  }

  public static class NotSuitable {
    private static final int BASE = 1000;
    public static final Response PLAN_CHANGE = new Response(BASE + 1, "Seems that you don't have a subsciption. Only subscribers can change their plans!");
    public static final Response PAYMENT_FAILURE_ON_PLAN_CHANGE = new Response(BASE + 2, "Your payment failed during plan changing! Please try again or use another card!");
    public static final Response EMPTY_URL_LIST = new Response(BASE + 3, "URL list is empty!");
    public static final Response TICKET = new Response(BASE + 5, "Ticket is not suitable!");
    public static final Response LINK = new Response(BASE + 7, "Link(s) is not suitable for this update!");
    public static final Response EMAIL = new Response(BASE + 15, "This email is reserved!");
  }

  public static class NotFound {
    private static final int BASE = 404;

    public static final Response SEARCH_NOT_FOUND = new Response("Nothing found!"); // not an error!

    public static final Response PLATFORM = new Response(BASE, "Platform not found!");

    public static final Response WORKSPACE = new Response(BASE, "Workspace not found!");
    public static final Response PLAN = new Response(BASE, "Plan not found!");
    public static final Response TICKET = new Response(BASE, "Ticket not found!");
    public static final Response COMMENT = new Response(BASE, "Comment not found!");
    public static final Response VOUCHER = new Response(BASE, "Voucher not found!");
    public static final Response ANNOUNCE = new Response(BASE, "Announce not found!");
    public static final Response ALARM = new Response(BASE, "Alarm not found!");
    public static final Response SMART_PRICE = new Response(BASE, "Smart price not found!");

    public static final Response USER = new Response(BASE, "User not found!");
    public static final Response EMAIL = new Response(BASE, "Email not found!");
    public static final Response MEMBERSHIP = new Response(BASE, "Membership not found!");
    public static final Response INVITATION = new Response(BASE, "Invitation not found!");
    public static final Response SUBSCRIPTION = new Response(BASE, "Subscription not found!");

    public static final Response PRODUCT = new Response(BASE, "Product not found!");
    public static final Response LINK = new Response(BASE, "Link not found!");

    public static final Response BRAND = new Response(BASE, "Brand not found!");
    public static final Response CATEGORY = new Response(BASE, "Category not found!");
    
    public static final Response HISTORY = new Response(BASE, "History not found!");
    public static final Response TRANSACTION = new Response(BASE, "Transaction not found!");

    public static final Response USED_SERVICE = new Response(BASE, "Used service not found!");
    
    public static final Response DATA = new Response(BASE, "Nothing found!");
  }

  public static class NotActive {
    private static final int BASE = 505;
    public static final Response INVITATION = new Response(BASE, "Invitation is not active!");
  }

}
