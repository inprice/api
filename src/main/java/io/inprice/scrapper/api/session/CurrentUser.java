package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.app.auth.SessionInfoForDB;
import io.inprice.scrapper.api.app.auth.SessionInfoForToken;
import io.inprice.scrapper.api.app.user.UserRole;

public class CurrentUser {

   private final static ThreadLocal<ThreadVariables> THREAD_VARIABLES = new ThreadLocal<ThreadVariables>() {
      @Override
      protected ThreadVariables initialValue() {
         return new ThreadVariables();
      }
   };

   static void set(SessionInfoForToken sestok, SessionInfoForDB sesdb) {
      THREAD_VARIABLES.get().set(sestok, sesdb);
   }

   public static Long getUserId() {
      return THREAD_VARIABLES.get().getFromDatabase().getUserId();
   }

   public static String getEmail() {
      return THREAD_VARIABLES.get().getFromToken().getEmail();
   }

   public static String getUserName() {
      return THREAD_VARIABLES.get().getFromToken().getUser();
   }

   public static Long getCompanyId() {
      return THREAD_VARIABLES.get().getFromDatabase().getCompanyId();
   }

   public static String getCompanyName() {
      return THREAD_VARIABLES.get().getFromToken().getCompany();
   }

   public static UserRole getRole() {
      return THREAD_VARIABLES.get().getFromToken().getRole();
   }

   public static void cleanup() {
      THREAD_VARIABLES.remove();
      THREAD_VARIABLES.set(null);
      THREAD_VARIABLES.remove();
   }

}
