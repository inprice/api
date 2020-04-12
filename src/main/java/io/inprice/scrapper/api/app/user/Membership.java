package io.inprice.scrapper.api.app.user;

import java.io.Serializable;
import java.util.UUID;

import io.inprice.scrapper.api.app.member.MemberRole;

public class Membership implements Serializable {

   private static final long serialVersionUID = -3674182848805470673L;

   private Long companyId;
   private String companyName;
   private MemberRole role;
   private String token;

   public Membership() {
   }

   public Membership(Long companyId, String companyName, MemberRole role) {
      this.companyId = companyId;
      this.companyName = companyName;
      this.role = role;
      this.token = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

   public String getCompanyName() {
      return companyName;
   }

   public void setCompanyName(String companyName) {
      this.companyName = companyName;
   }

   public MemberRole getRole() {
      return role;
   }

   public void setRole(MemberRole role) {
      this.role = role;
   }

   public String getToken() {
      return token;
   }

   @Override
   public String toString() {
      return "[companyId=" + companyId + ", companyName=" + companyName + ", role=" + role + ", token=" + token
            + "]";
   }

}
