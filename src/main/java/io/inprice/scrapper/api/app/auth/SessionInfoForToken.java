package io.inprice.scrapper.api.app.auth;

import java.io.Serializable;
import java.util.UUID;

import io.inprice.scrapper.api.app.user.UserRole;

public class SessionInfoForToken implements Serializable {

   private static final long serialVersionUID = 8351087616755687271L;

   private String hash;
   private String user;
   private String email;
   private String company;
   private UserRole role;

   public SessionInfoForToken() {
   }

   public SessionInfoForToken(String user, String email, String company, UserRole role) {
      this.email = email;
      this.user = user;
      this.company = company;
      this.role = role;
      this.hash = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
   }

   public String getHash() {
      return hash;
   }

   public String getEmail() {
      return email;
   }

   public String getUser() {
      return user;
   }

   public String getCompany() {
      return company;
   }

   public UserRole getRole() {
      return role;
   }

   @Override
   public String toString() {
      return "[company=" + company + ", email=" + email + ", role=" + role + ", user=" + user + "]";
   }

}