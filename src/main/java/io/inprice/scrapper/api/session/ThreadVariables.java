package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.app.auth.AuthUser;
import io.inprice.scrapper.api.app.user.UserCompany;

class ThreadVariables {

   private AuthUser authUser;
   private UserCompany company;

   ThreadVariables() {
   }

   public void setAuthUser(AuthUser authUser, UserCompany userCompany) {
      this.authUser = authUser;
      this.company = userCompany;
   }

   public AuthUser getAuthUser() {
      return authUser;
   }

   public UserCompany getCompany() {
      return company;
   }

}
