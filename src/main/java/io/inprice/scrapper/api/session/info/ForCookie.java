package io.inprice.scrapper.api.session.info;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.inprice.scrapper.api.app.user.UserRole;

/**
 * Used in session cookie
 */
public class ForCookie implements Serializable {

   private static final long serialVersionUID = -2758435636435796934L;

   @JsonProperty("e")
   private String email;

   @JsonProperty("r")
   private UserRole role;

   @JsonProperty("h")
   private String hash;

   public ForCookie() {
   }

   public ForCookie(String email, UserRole role) {
      this.email = email;
      this.role = role;
      this.hash = UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
   }

   public String getEmail() {
      return email;
   }

   public UserRole getRole() {
      return role;
   }

   public String getHash() {
      return hash;
   }

   @Override
   public String toString() {
      return "[email=" + email + ", role=" + role + "]";
   }

}