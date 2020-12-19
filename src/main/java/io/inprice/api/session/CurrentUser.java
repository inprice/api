package io.inprice.api.session;

import io.inprice.api.session.info.ForRedis;
import io.inprice.common.meta.UserRole;

public class CurrentUser {

  private final static ThreadLocal<ThreadVariables> THREAD_VARIABLES = new ThreadLocal<ThreadVariables>() {
    @Override
    protected ThreadVariables initialValue() {
      return new ThreadVariables();
    }
  };

  static void set(ForRedis forRedis, int sessionNo) {
    THREAD_VARIABLES.get().set(forRedis, sessionNo);
  }

  public static int getSessionNo() {
    return THREAD_VARIABLES.get().getSessionNo();
  }

  public static String getSessionHash() {
    return THREAD_VARIABLES.get().getSession().getHash();
  }

  public static Long getUserId() {
    return THREAD_VARIABLES.get().getSession().getUserId();
  }

  public static String getEmail() {
    return THREAD_VARIABLES.get().getSession().getEmail();
  }

  public static String getUserName() {
    return THREAD_VARIABLES.get().getSession().getUser();
  }

  public static String getUserTimezone() {
    return THREAD_VARIABLES.get().getSession().getTimezone();
  }

  public static Long getAccountId() {
    return THREAD_VARIABLES.get().getSession().getAccountId();
  }

  public static String getAccountName() {
    return THREAD_VARIABLES.get().getSession().getAccount();
  }

  public static UserRole getRole() {
    return THREAD_VARIABLES.get().getSession().getRole();
  }

  public static void cleanup() {
    THREAD_VARIABLES.set(null);
    THREAD_VARIABLES.remove();
  }

}
