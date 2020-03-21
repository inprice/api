package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.user.UserRole;

public class MemberDTO {

   private String email;
   private UserRole role;

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public UserRole getRole() {
      return role;
   }

   public void setRole(UserRole role) {
      this.role = role;
   }

   @Override
   public String toString() {
      return "email=" + email + ", role=" + role;
   }

}
