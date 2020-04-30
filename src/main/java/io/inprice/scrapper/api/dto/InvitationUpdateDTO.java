package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import io.inprice.scrapper.api.app.user.UserRole;

public class InvitationUpdateDTO implements Serializable {

   private static final long serialVersionUID = -7922528699777216078L;

   private Long id;
   private UserRole role;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public UserRole getRole() {
      return role;
   }

   public void setRole(UserRole role) {
      this.role = role;
   }

   @Override
   public String toString() {
      return "[id=" + id + ", role=" + role + "]";
   }

}