package io.inprice.api.consts;

import java.util.Set;

import io.inprice.common.helpers.GlobalConsts;

public class Consts {

	public static final String SESSION = "INPRICE_AT";
  public static final String SUPER_SESSION = "INPRICE_SAT";
  public static final String SESSION_NO = "X-Session";

  public static final String IP = "ip";
  public static final String TIMEZONE = "timezone";
  public static final String CURRENCY_CODE = "currencyCode";
  public static final String CURRENCY_FORMAT = "currencyFormat";

  public static final int LOWER_ROW_LIMIT_FOR_LISTS = 25;
  public static final int UPPER_ROW_LIMIT_FOR_LISTS = 100;

  public static class Env {
  	public static final String DEV = "dev";
  	public static final String TEST = "test";
  	public static final String PROD = "prod";
  }

  public static Set<String> EXCLUSIVE_EMAILS = Set.of("super@inprice.io", "admin@inprice.io", GlobalConsts.DEMO_ACCOUNT);

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

    public static class Workspace {
      public static final String BASE = "/workspace";
      public static final String GEO_INFO = BASE + "/geo";
    }

    public static class User {
      public static final String BASE = "/user";
      public static final String INVITATIONS = BASE + "/invitations";
      public static final String MEMBERSHIPS = BASE + "/memberships";
      public static final String OPENED_SESSIONS = BASE + "/opened-sessions";
      public static final String UPDATE_INFO = BASE + "/update-info";
      public static final String CHANGE_PASSWORD = BASE + "/change-password";
      public static final String ACCEPT_INVITATION = BASE + "/accept-invitation";
      public static final String REJECT_INVITATION = BASE + "/reject-invitation";
      public static final String LEAVE_MEMBERSHIP = BASE + "/leave-membership";
      public static final String CLOSE_ALL_SESSIONS = BASE + "/close-all-sessions";
    }

    public static class Membership {
      public static final String BASE = "/membership";
      public static final String PAUSE = BASE + "/pause";
      public static final String RESUME = BASE + "/resume";
      public static final String CHANGE_ROLE = BASE + "/change-role";
    }

    public static class Product {
      public static final String BASE = "/product";
      public static final String SEARCH = BASE + "s/search";
      public static final String ID_NAME_PAIRS = BASE + "/pairs";
      public static final String LINKS = BASE + "/links";
      public static final String ADD_LINKS = BASE + "/add-links";
      public static final String ALARM_ON = BASE + "/alarm/on";
      public static final String ALARM_OFF = BASE + "/alarm/off";
    }

    public static class Link {
      public static final String BASE = "/link";
      public static final String SEARCH = BASE + "s/search";
      public static final String DETAILS = BASE + "/details";
      public static final String MOVE = BASE + "/move";
      public static final String ALARM_ON = BASE + "/alarm/on";
      public static final String ALARM_OFF = BASE + "/alarm/off";
    }

    public static class Subscription {
      public static final String BASE = "/subscription";
      public static final String CANCEL = BASE + "/cancel";
      public static final String SAVE_INFO = BASE + "/save-info";
      public static final String GET_INFO = BASE + "/get-info";
      public static final String START_FREE_USE = BASE + "/free-use";
      public static final String CREATE_CHECKOUT = BASE + "/create-checkout";
      public static final String CANCEL_CHECKOUT = BASE + "/cancel-checkout";
      public static final String CHANGE_PLAN = BASE + "/change-plan";
    }

    public static class Voucher {
    	public static final String BASE = "/voucher";
    	public static final String APPLY = BASE + "/apply";
    }

    public static class Ticket {
      public static final String BASE = "/ticket";
      public static final String SEARCH = BASE + "s/search";
      public static final String TOGGLE_SEEN_VALUE = BASE + "/seen";
      public static final String COMMENT = BASE + "/comment";
    }

    public static class Announce {
      public static final String BASE = "/announce";
    	public static final String SEARCH = BASE + "s/search";
    	public static final String NEW_ANNOUNCES = BASE + "s/new";
    }

    public static class Alarm {
      public static final String BASE = "/alarm";
    	public static final String SEARCH = BASE + "s/search";
      public static final String DETAILS = BASE + "/details";
      public static final String ID_NAME_PAIRS = BASE + "/pairs";
      public static final String ALARM_OFF = BASE + "/off";
    }

    public static class Definitions {
      public static final String BASE = "/def";
    	public static final String BRAND = BASE + "/brand";
    	public static final String CATEGORY = BASE + "/category";
    }

    public static class SmartPrice {
      public static final String BASE = "/smart-price";
    	public static final String TEST = BASE + "/test";
    	public static final String LIST = BASE + "s";
    	public static final String SEARCH = BASE + "s/search";
    }

    public static class Dashboard {
      public static final String BASE = "/dashboard";
      public static final String REFRESH = BASE + "/refresh";
    }

    public static class Exim {
      public static final String BASE = "/exim";
      public static final String PRODUCT = BASE + "/product";
      public static final String LINK = BASE + "/link";
      public static final String BRAND = BASE + "/brand";
      public static final String CATEGORY = BASE + "/category";
    }

    public static class System {
    	public static final String BASE = "/app";
    	public static final String PLANS = BASE + "/plans";
    	public static final String REFRESH_SESSION = BASE + "/refresh-session";
    	public static final String STATISTICS = BASE + "/statistics";
    }

    public static class Super {
      public static final String BASE = "/sys";

      public static class Dashboard {
        public static final String _BASE = BASE + "/dashboard";
        public static final String REFRESH = _BASE + "/refresh";
      }
  
      public static class Workspace {
      	public static final String _BASE = BASE + "/workspace";
      	public static final String SEARCH = _BASE + "s/search";
        public static final String AL_SEARCH = _BASE + "/search-logs";
        public static final String ID_NAME_PAIRS = _BASE + "/id-name-pairs";
        public static final String BAN = _BASE + "/ban";
        public static final String REVOKE_BAN = BAN + "-revoke";
        public static final String WORKSPACE_USERS = _BASE + "/users";
      	
        public static final String DETAILS = _BASE + "/details";
        public static final String MEMBER_LIST = DETAILS + "/members";
        public static final String HISTORY = DETAILS + "/history";
      	public static final String TRANSACTION_LIST = DETAILS + "/transactions";

      	public static final String BIND = _BASE + "/bind";
        public static final String UNBIND = _BASE + "/unbind";
        public static final String VOUCHER = _BASE + "/voucher";
      }

      public static class User {
      	public static final String _BASE = BASE + "/user";
      	public static final String SEARCH = _BASE + "s/search";
        public static final String AL_SEARCH = _BASE + "/search-logs";
        public static final String BAN = _BASE + "/ban";
        public static final String REVOKE_BAN = BAN + "-revoke";
        public static final String USER_WORKSPACES = _BASE + "/workspaces";

        public static final String USED_SERVICE = _BASE + "/used-service";
        public static final String USED_SERVICE_TOGGLE = USED_SERVICE + "/toggle";
        
        public static final String SESSION = _BASE + "/session";
        public static final String TERMINATE_SESSION = SESSION + "/terminate";
        public static final String TERMINATE_ALL_SESSIONS = SESSION + "/terminate-all";

        public static final String DETAILS = _BASE + "/details";
        public static final String MEMBERSHIP_LIST = DETAILS + "/memberships";
        public static final String SESSION_LIST = DETAILS + "/sessions";
      }

      public static class Link {
        public static final String _BASE = BASE + "/link";
        public static final String SEARCH = _BASE + "s/search";
        public static final String DETAILS = _BASE + "/details";
        public static final String CHANGE_STATUS = _BASE + "/change-status";
        public static final String UNDO = _BASE + "/undo";
      }
      
      public static class Ticket {
      	public static final String _BASE = BASE + "/ticket";
      	public static final String SEARCH = _BASE + "s/search";
      	public static final String CHANGE_STATUS = _BASE + "/change-status";
      	public static final String TOGGLE_SEEN_VALUE = _BASE + "/seen";
      	public static final String COMMENT = _BASE + "/comment";
      }

      public static class Announce {
        public static final String _BASE = BASE + "/announce";
      	public static final String SEARCH = _BASE + "s/search";
      }

      public static class Platform {
        public static final String _BASE = BASE + "/platform";
        public static final String SEARCH = _BASE + "s/search";
        public static final String TOGGLE_PARKED = _BASE + "/toggle-parked";
        public static final String TOGGLE_BLOCKED = _BASE + "/toggle-blocked";
      }

    }

    public static class Report {
      public static final String BASE = "/reports";
      public static final String PRODUCT = BASE + "/product";
    	public static final String LINK = BASE + "/link";
    }
    
  }

}
