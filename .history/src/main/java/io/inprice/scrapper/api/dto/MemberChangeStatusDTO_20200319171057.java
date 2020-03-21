package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.user.UserStatus;

public class MemberChangeStatusDTO {

   private String adminName;
   private String companyName;
   private String email;
   private UserStatus status;

   public String getAdminName() {
      return adminName;
   }

   public void setAdminName(String adminName) {
      this.adminName = adminName;
   }

   public String getCompanyName() {
      return companyName;
   }

   public void setCompanyName(String companyName) {
      this.companyName = companyName;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public UserStatus getStatus() {
      return status;
   }

   public void setStatus(UserStatus status) {
      this.status = status;
   }

   @Override
   public String toString() {
      return "MemberChangeStatusDTO [adminName=" + adminName + ", companyName=" + companyName + ", email=" + email
            + ", status=" + status + "]";
   }

}
