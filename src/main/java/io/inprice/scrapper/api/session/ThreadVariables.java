package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.info.AuthUser;

class ThreadVariables {

   private AuthUser authUser;

   ThreadVariables() {
   }

   public void setAuthUser(AuthUser authUser) {
      this.authUser = authUser;
   }

   public AuthUser getAuthUser() {
      return authUser;
   }

}
