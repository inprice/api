package io.inprice.scrapper.api.session.info;

import java.io.Serializable;

import io.inprice.scrapper.api.app.user.UserRole;

/**
 * Used in responses from successful Login
 */
public class ForResponse implements Serializable {

   private static final long serialVersionUID = -3414991620052194958L;

   private String user;
   private String email;
   private String company;
   private UserRole role;

   public ForResponse() {
   }

   public ForResponse(ForResponse forResponse) {
      this.user = forResponse.getUser();
      this.email = forResponse.getEmail();
      this.company = forResponse.getCompany();
      this.role = forResponse.getRole();
   }

   public ForResponse(ForCookie forCookie, String user, String company) {
      this.user = user;
      this.email = forCookie.getEmail();
      this.company = company;
      this.role = forCookie.getRole();
   }

   public String getUser() {
      return user;
   }

   public String getEmail() {
      return email;
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