package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.member.MemberStatus;

public class MemberChangeStatusDTO {

   private String email;
   private MemberStatus status;

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public MemberStatus getStatus() {
      return status;
   }

   public void setStatus(MemberStatus status) {
      this.status = status;
   }

   @Override
   public String toString() {
      return "email=" + email + ", status=" + status;
   }

}
