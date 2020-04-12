package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.app.user.Membership;
import io.inprice.scrapper.api.info.AuthUser;

class ThreadVariables {

   private AuthUser authUser;
   private Membership membership;

   ThreadVariables() {
   }

   public void setAuthUser(AuthUser authUser, Long companyId) {
      this.authUser = authUser;
      this.membership = authUser.getMemberships().get(companyId);
   }

   public AuthUser getAuthUser() {
      return authUser;
   }

   public Membership getMembership() {
      return membership;
   }

}
