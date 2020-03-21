package io.inprice.scrapper.api.app.user;

import java.io.Serializable;
import java.util.Date;

public class User implements Serializable {

   private static final long serialVersionUID = 1753526228909974777L;

   private Long id;
   private String email;
   private String name;
   private String passwordHash;
   private String passwordSalt;
   private Date createdAt;

   //transient
   private UserRole role;

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

   public String getPasswordHash() {
      return passwordHash;
   }

   public void setPasswordHash(String passwordHash) {
      this.passwordHash = passwordHash;
   }

   public String getPasswordSalt() {
      return passwordSalt;
   }

   public void setPasswordSalt(String passwordSalt) {
      this.passwordSalt = passwordSalt;
   }

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   public UserRole getRole() {
      return role;
   }

   public void setRole(UserRole role) {
      this.role = role;
   }

}
