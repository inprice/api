package io.inprice.scrapper.api.app.user;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.inprice.scrapper.api.app.member.MemberRole;

@JsonIgnoreProperties(value = { "id" })
public class UserCompany implements Serializable {

   private static final long serialVersionUID = -3674182848805470673L;

   private Long id;
   private String hash;
   private String name;
   private MemberRole role;

   public UserCompany() {
   }

   public UserCompany(Long id, String name, MemberRole role) {
      this.id = id;
      this.name = name;
      this.role = role;
      this.hash = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
   }

   public Long getId() {
      return id;
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

   public String getHash() {
      return hash;
   }

   @Override
   public String toString() {
      return "[hash=" + hash + ", name=" + name + ", role=" + role + "]";
   }

}
