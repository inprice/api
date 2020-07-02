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

   static void set(ForRedis forRedis) {
      THREAD_VARIABLES.get().set(forRedis);
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

   public static Long getCompanyId() {
      return THREAD_VARIABLES.get().getSession().getCompanyId();
   }

   public static String getCompanyName() {
      return THREAD_VARIABLES.get().getSession().getCompany();
   }

   public static UserRole getRole() {
      return THREAD_VARIABLES.get().getSession().getRole();
   }

   public static Long getPlanId() {
      return THREAD_VARIABLES.get().getSession().getPlanId();
   }

   public static void cleanup() {
      THREAD_VARIABLES.set(null);
      THREAD_VARIABLES.remove();
   }

}
