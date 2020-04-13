package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.app.user.UserCompany;
import io.inprice.scrapper.api.info.AuthUser;

class ThreadVariables {

   private AuthUser authUser;
   private UserCompany company;
   private Long companyId;

   ThreadVariables() {
   }

   public void setAuthUser(AuthUser authUser, Long companyId) {
      this.authUser = authUser;
      this.company = authUser.getCompanies().get(companyId);
   }

   public AuthUser getAuthUser() {
      return authUser;
   }

   public UserCompany getCompany() {
      return company;
   }

   public Long getCompanyId() {
      return companyId;
   }

}
