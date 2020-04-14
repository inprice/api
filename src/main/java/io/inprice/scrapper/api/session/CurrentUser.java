package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.app.auth.AuthUser;
import io.inprice.scrapper.api.app.auth.UserSession;
import io.inprice.scrapper.api.app.member.MemberRole;

public class CurrentUser {

   private final static ThreadLocal<ThreadVariables> THREAD_VARIABLES = new ThreadLocal<ThreadVariables>() {
      @Override
      protected ThreadVariables initialValue() {
         return new ThreadVariables();
      }
   };

   static void set(AuthUser authUser, UserSession session, String companyName, MemberRole role) {
      THREAD_VARIABLES.get().set(authUser, session, companyName, role);
   }

   public static Long getUserId() {
      return THREAD_VARIABLES.get().getSession().getUserId();
   }

   public static String getEmail() {
      return THREAD_VARIABLES.get().getAuthUser().getEmail();
   }

   public static String getUserName() {
      return THREAD_VARIABLES.get().getAuthUser().getName();
   }

   public static Long getCompanyId() {
      return THREAD_VARIABLES.get().getSession().getCompanyId();
   }

   public static String getCompanyName() {
      return THREAD_VARIABLES.get().getCompanyName();
   }

   public static MemberRole getRole() {
      return THREAD_VARIABLES.get().getRole();
   }

   public static void cleanup() {
      THREAD_VARIABLES.remove();
      THREAD_VARIABLES.set(null);
      THREAD_VARIABLES.remove();
   }

}
