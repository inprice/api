package io.inprice.api.consts;

public class Consts {

  public static final String SESSION = "INPRICE_AT";
  public static final String SESSION_NO = "X-Session";

  public static final String IP = "ip";
  public static final String TIMEZONE = "timezone";
  public static final String CURRENCY_CODE = "currencyCode";
  public static final String CURRENCY_FORMAT = "currencyFormat";

  public static final int ROW_LIMIT_FOR_LISTS = 25;

  public static class Paths {

    public static class Auth {
      public static final String LOGIN = "/login";
      public static final String LOGOUT = "/logout";
      public static final String FORGOT_PASSWORD = "/forgot-password";
      public static final String RESET_PASSWORD = "/reset-password";
      public static final String REQUEST_REGISTRATION = "/request-registration";
      public static final String COMPLETE_REGISTRATION = "/complete-registration";
      public static final String ACCEPT_INVITATION = "/accept-invitation";
    }

    public static final String ADMIN_BASE = "/admin";

    public static class AdminUser {
      public static final String BASE = ADMIN_BASE + "/user";
      public static final String PASSWORD = BASE + "/password";
      public static final String TOGGLE_STATUS = BASE + "/toggle";
      public static final String SEARCH = BASE + "s/search";
    }

    public static class Company {
      public static final String BASE = "/company";
      public static final String GEO_INFO = BASE + "/geo";
      public static final String DELETE = BASE + "/delete";
    }

    public static class User {
      public static final String BASE = "/user";
      public static final String PASSWORD = BASE + "/change-password";
      public static final String UPDATE = BASE + "/update";
      public static final String INVITATIONS = BASE + "/invitations";
      public static final String ACCEPT_INVITATION = BASE + "/accept-invitation";
      public static final String REJECT_INVITATION = BASE + "/reject-invitation";
      public static final String MEMBERSHIPS = BASE + "/memberships";
      public static final String LEAVE_MEMBERSHIP = BASE + "/leave-member";
      public static final String OPENED_SESSIONS = BASE + "/opened-sessions";
      public static final String CLOSE_ALL_SESSIONS = BASE + "/close-all-sessions";
    }

    public static class Member {
      public static final String BASE = "/member";
      public static final String PAUSE = BASE + "/pause";
      public static final String RESUME = BASE + "/resume";
      public static final String DELETE = BASE + "/delete";
      public static final String CHANGE_ROLE = BASE + "/change-role";
    }

    public static class Product {
      public static final String BASE = "/product";
      public static final String EVERYTHING = BASE + "/everything";
      public static final String SEARCH = BASE + "s/search";
      public static final String TOGGLE_STATUS = BASE + "/toggle";

      public static final String IMPORT_BASE = BASE + "/import";
      public static final String IMPORTED_PRODUCTS = IMPORT_BASE + "s/search";
      public static final String IMPORT_CSV = IMPORT_BASE + "/csv";
      public static final String IMPORT_URL_LIST = IMPORT_BASE + "/url-list";
      public static final String IMPORT_EBAY_SKU = IMPORT_BASE + "/ebay-sku";
      public static final String IMPORT_AMAZON_ASIN = IMPORT_BASE + "/amazon-asin";
    }

    public static class Link {
      public static final String BASE = "/link";
      public static final String SEARCH = BASE + "s/search";
      public static final String RENEW = BASE + "/renewed";
      public static final String PAUSE = BASE + "/paused";
      public static final String RESUME = BASE + "/resumed";
    }

    public static class Subscription {
      public static final String BASE = "/subscription";
      public static final String CANCEL = BASE + "/cancel";
      public static final String CREATE_SESSION = BASE + "/create-session";
      public static final String TRANSACTIONS = BASE + "/trans";
      public static final String SAVE_INFO = BASE + "/save-info";
    }

    public static class Coupon {
      public static final String BASE = "/coupon";
      public static final String APPLY = BASE + "/apply";
    }

    public static class Tag {
      public static final String BASE = "/tag";
      public static final String PRODUCT = BASE + "/product";
    }

    public static class Dashboard {
      public static final String BASE = "/dashboard";
      public static final String REFRESH = BASE + "/refresh";
    }

    public static class System {
      public static final String BASE = "/app";
      public static final String PLANS = BASE + "/plans";
    }

    public static class Webhook {
      public static final String STRIPE = "/stripe/webhook";
    }

  }

}
