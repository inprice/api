package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.app.auth.AuthUser;
import io.inprice.scrapper.api.app.auth.UserSession;
import io.inprice.scrapper.api.app.member.MemberRole;

class ThreadVariables {

   private AuthUser authUser;
   private UserSession session;
   private String companyName;
   private MemberRole role;

   ThreadVariables() {
   }

   void set(AuthUser authUser, UserSession session, String companyName, MemberRole role) {
      this.authUser = authUser;
      this.session = session;
      this.companyName = companyName;
      this.role = role;
   }

   AuthUser getAuthUser() {
      return authUser;
   }

   UserSession getSession() {
      return session;
   }

   String getCompanyName() {
      return companyName;
   }

   MemberRole getRole() {
      return role;
   }

}
