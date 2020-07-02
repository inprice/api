package io.inprice.api.consts;

import io.inprice.common.meta.LookupType;

public class Consts {

  public static final String SESSION = "INPRICE_AT";
  public static final String SESSION_NO = "X-Session";

  public static final String IP = "ip";
  public static final String TIMEZONE = "timezone";
  public static final String CURRENCY_CODE = "currencyCode";
  public static final String CURRENCY_FORMAT = "currencyFormat";

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
      public static final String COUPONS = BASE + "/coupons";
      public static final String APPLY_COUPON = BASE + "/apply-coupon";
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
      public static final String LEAVE_MEMBERSHIP = BASE + "/leave-membership";
      public static final String OPENED_SESSIONS = BASE + "/opened-sessions";
      public static final String CLOSE_ALL_SESSIONS = BASE + "/close-all-sessions";
    }

    public static class Membership {
      public static final String BASE = "/membership";
      public static final String PAUSE = BASE + "/pause";
      public static final String RESUME = BASE + "/resume";
      public static final String DELETE = BASE + "/delete";
      public static final String CHANGE_ROLE = BASE + "/change-role";
    }

    public static class Product {
      public static final String BASE = "/product";
      public static final String SEARCH = BASE + "s/search";
      public static final String TOGGLE_STATUS = BASE + "/toggle";

      public static final String IMPORT_BASE = BASE + "/import";
      public static final String IMPORTED_PRODUCTS = IMPORT_BASE + "s/search";
      public static final String IMPORT_CSV = IMPORT_BASE + "/csv";
      public static final String IMPORT_URL_LIST = IMPORT_BASE + "/url-list";
      public static final String IMPORT_EBAY_SKU = IMPORT_BASE + "/ebay-sku";
      public static final String IMPORT_AMAZON_ASIN = IMPORT_BASE + "/amazon-asin";
    }

    public static class Competitor {
      public static final String BASE = "/competitor";
      public static final String SEARCH = BASE + "/search";
      public static final String RENEW = BASE + "/renew";
      public static final String PAUSE = BASE + "/pause";
      public static final String RESUME = BASE + "/resume";
    }

    public static class Lookup {
      public static final String BASE = "/lookup";
      public static final String BRAND = BASE + "/" + LookupType.BRAND.name().toLowerCase();
      public static final String CATEGORY = BASE  + LookupType.CATEGORY.name().toLowerCase();
    }

    public static class Dashboard {
      public static final String BASE = "/dashboard";
      public static final String REFRESH = BASE + "/refresh";
    }

  }

}
