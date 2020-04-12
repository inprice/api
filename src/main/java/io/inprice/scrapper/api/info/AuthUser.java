package io.inprice.scrapper.api.info;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import io.inprice.scrapper.api.app.user.Membership;

public class AuthUser implements Serializable {

   private static final long serialVersionUID = 5763780271600754333L;

   private Long userId;
   private String email;
   private String userName;
   private Map<Long, Membership> memberships = new HashMap<>();

   public Long getUserId() {
      return userId;
   }

   public void setUserId(Long userId) {
      this.userId = userId;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public Map<Long, Membership> getMemberships() {
      return memberships;
   }

   public void setMemberships(Map<Long, Membership> memberships) {
      this.memberships = memberships;
   }

   @Override
   public String toString() {
      return "[email=" + email + ", userId=" + userId + ", userName=" + userName + "]";
   }

}
