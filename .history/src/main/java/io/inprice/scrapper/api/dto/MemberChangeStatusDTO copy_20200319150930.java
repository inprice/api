package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;

public class MemberChangeStatusDTO copy {

   private String email;
   private UserStatus status;

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
      return "email=" + email + ", status=" + status;
   }

}
