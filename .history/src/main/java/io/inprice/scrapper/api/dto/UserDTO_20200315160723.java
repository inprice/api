package io.inprice.scrapper.api.dto;

import java.io.Serializable;

/**
 * Used for handling user info from client side
 */
public class UserDTO implements Serializable {

   private static final long serialVersionUID = -4510116778307627456L;

   private Long id;
   private UserStatus status;
   private UserRole role;
   private String fullName;
   private String email;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public UserStatus getStatus() {
      return status;
   }

   public void setStatus(UserStatus status) {
      this.status = status;
   }

   public UserRole getRole() {
      return role;
   }

   public void setRole(UserRole role) {
      this.role = role;
   }

   public String getFullName() {
      return fullName;
   }

   public void setFullName(String fullName) {
      this.fullName = fullName;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

}
