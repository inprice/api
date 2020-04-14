package io.inprice.scrapper.api.consts;

public class Consts {

   public static final String USER_NO = "User";
   public static final String COMPANY_NO = "Company";

   public static class Cookie {
      public static final int LIMIT = 32;
      public static final String SESSION = "INPRICE_AT_";
   }

   public static class Paths {

      public static class Auth {
         public static final String LOGIN = "/login";
         public static final String LOGOUT = "/logout";
         public static final String FORGOT_PASSWORD = "/forgot-password";
         public static final String RESET_PASSWORD = "/reset-password";
         public static final String REQUEST_REGISTRATION = "/request-registration";
         public static final String COMPLETE_REGISTRATION = "/complete-registration";
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
      }

      public static class User {
         public static final String BASE = "/user";
         public static final String PASSWORD = BASE + "/change-password";
         public static final String SWITCH_COMPANY = BASE + "/switch-company";
      }

      public static class Invitation {
         public static final String BASE = "/invitation";
         public static final String SEND = BASE + "/send";
         public static final String RESEND = BASE + "/resend";
         public static final String ACCEPT_NEW = BASE + "/accept-new";
         public static final String ACCEPT_EXISTING = BASE + "/accept";
         public static final String REJECT_EXISTING = BASE + "/reject";
      }

      public static class Member {
         public static final String BASE = "/member";
         public static final String TOGGLE_STATUS = BASE + "/toggle";
         public static final String CHANGE_ROLE = BASE + "/change-role";
         public static final String CHANGE_STATUS = BASE + "/change-status";
      }

      public static class Product {
         public static final String BASE = "/product";
         public static final String SEARCH = BASE + "s/search";
         public static final String TOGGLE_STATUS = BASE + "/toggle";

         public static final String IMPORT_BASE = BASE + "/import";
         public static final String IMPORT_CSV = IMPORT_BASE + "/csv";
         public static final String IMPORT_URL_LIST = IMPORT_BASE + "/url";
         public static final String IMPORT_EBAY_SKU_LIST = IMPORT_BASE + "/ebay";
         public static final String IMPORT_AMAZON_ASIN_LIST = IMPORT_BASE + "/amazon";
      }

      public static class Link {
         public static final String BASE = "/link";
         public static final String RENEW = BASE + "/renew";
         public static final String PAUSE = BASE + "/pause";
         public static final String RESUME = BASE + "/resume";
      }

      public static class Ticket {
         public static final String BASE = "/ticket";
      }

      public static class Misc {
         public static final String BASE = "/misc";
         public static final String DASHBOARD = BASE + "/dashboard";
      }

   }

}
