package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;

public class InvitationUpdateDTO implements Serializable {

   private static final long serialVersionUID = -7922528699777216078L;

   private Long id;
   private UserRole role;
   private UserStatus status;

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

   public UserStatus getStatus() {
      return status;
   }

   public void setStatus(UserStatus status) {
      this.status = status;
   }

   @Override
   public String toString() {
      return "[id=" + id + ", role=" + role + ", status=" + status + "]";
   }

}