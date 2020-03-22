package io.inprice.scrapper.api.info;

import java.io.Serializable;

import io.inprice.scrapper.api.app.member.MemberRole;

public class AuthUser implements Serializable {

   private static final long serialVersionUID = 5763780271600754333L;

   private Long id;
   private String email;
   private String name;
   private MemberRole role;
   private Long companyId;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public MemberRole getRole() {
      return role;
   }

   public void setRole(MemberRole role) {
      this.role = role;
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

}
