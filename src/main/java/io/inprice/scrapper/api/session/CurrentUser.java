package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.app.member.MemberRole;
import io.inprice.scrapper.api.info.AuthUser;

public class CurrentUser {

   private final static ThreadLocal<ThreadVariables> THREAD_VARIABLES = new ThreadLocal<ThreadVariables>() {
      @Override
      protected ThreadVariables initialValue() {
         return new ThreadVariables();
      }
   };

   static void setAuthUser(AuthUser authUser) {
      THREAD_VARIABLES.get().setAuthUser(authUser);
   }

   public static Long getUserId() {
      return THREAD_VARIABLES.get().getAuthUser().getUserId();
   }

   public static String getEmail() {
      return THREAD_VARIABLES.get().getAuthUser().getEmail();
   }

   public static String getUserName() {
      return THREAD_VARIABLES.get().getAuthUser().getUserName();
   }

   public static Long getCompanyId() {
      return THREAD_VARIABLES.get().getAuthUser().getCompanyId();
   }

   public static String getCompanyName() {
      return THREAD_VARIABLES.get().getAuthUser().getCompanyName();
   }

   public static MemberRole getRole() {
      return MemberRole.valueOf(THREAD_VARIABLES.get().getAuthUser().getRole());
   }

   public static void cleanup() {
      THREAD_VARIABLES.remove();
      THREAD_VARIABLES.set(null);
      THREAD_VARIABLES.remove();
   }

}
