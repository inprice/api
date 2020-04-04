package io.inprice.scrapper.api.dto;

import java.io.Serializable;
import java.util.Date;

import io.inprice.scrapper.api.app.member.MemberRole;

public class InvitationDTO implements Serializable {

   private static final long serialVersionUID = 2545602928755294073L;

   private Long companyId;
   private String email;
   private MemberRole role;
   private Date createdAt = new Date();

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

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

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   @Override
   public String toString() {
      return "InvitationDTO [companyId=" + companyId + ", email=" + email + ", role=" + role + "]";
   }

}
