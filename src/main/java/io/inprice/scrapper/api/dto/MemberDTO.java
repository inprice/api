package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.member.MemberRole;

public class MemberDTO {

   private String email;
   private MemberRole role;

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public MemberRole getRole() {
      return role;
   }

   public void setRole(MemberRole role) {
      this.role = role;
   }

   @Override
   public String toString() {
      return "MemberDTO [email=" + email + ", role=" + role + "]";
   }

}
