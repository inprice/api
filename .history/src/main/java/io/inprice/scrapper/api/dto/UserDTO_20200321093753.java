package io.inprice.scrapper.api.dto;

import java.io.Serializable;

/**
 * Used for handling user info from client side
 */
public class UserDTO implements Serializable {

   private static final long serialVersionUID = -4510116778307627456L;

   private Long id;
   private String email;
   private String name;
   private String password;
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

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

}
