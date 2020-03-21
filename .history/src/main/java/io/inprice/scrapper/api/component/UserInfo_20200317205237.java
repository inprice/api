package io.inprice.scrapper.api.component;

import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.info.AuthUser;

public class UserInfo {

   private final static ThreadLocal<ThreadVariables> THREAD_VARIABLES = new ThreadLocal<ThreadVariables>() {
      @Override
      protected ThreadVariables initialValue() {
         return new ThreadVariables();
      }
   };

   static void setAuthUser(AuthUser authUser) {
      THREAD_VARIABLES.get().setAuthUser(authUser);
   }

   public static Long getId() {
      return THREAD_VARIABLES.get().getAuthUser().getId();
   }

   public static UserRole getRole() {
      return THREAD_VARIABLES.get().getAuthUser().getRole();
   }

   public static Long getCompanyId() {
      return THREAD_VARIABLES.get().getAuthUser().getCompanyId();
   }

   public static void cleanup() {
      THREAD_VARIABLES.set(null);
      THREAD_VARIABLES.remove();
   }

}
