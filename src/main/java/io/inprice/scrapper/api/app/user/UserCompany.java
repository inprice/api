package io.inprice.scrapper.api.app.user;

import java.io.Serializable;
import java.util.UUID;

import io.inprice.scrapper.api.app.member.MemberRole;

public class UserCompany implements Serializable {

   private static final long serialVersionUID = -3674182848805470673L;

   private Long id;
   private String name;
   private MemberRole role;
   private String token;

   public UserCompany() {
   }

   public UserCompany(Long id, String name, MemberRole role) {
      this.id = id;
      this.name = name;
      this.role = role;
      this.token = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
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

   public String getToken() {
      return token;
   }

   @Override
   public String toString() {
      return "[id=" + id + ", name=" + name + ", role=" + role + ", token=" + token + "]";
   }

}
