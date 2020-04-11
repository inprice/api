package io.inprice.scrapper.api.info;

import java.io.Serializable;

public class AuthUser implements Serializable {

   private static final long serialVersionUID = 5763780271600754333L;

   private Long userId;
   private String email;
   private String userName;
   private Long companyId;
   private String companyName;
   private String role;

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

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

   public String getCompanyName() {
      return companyName;
   }

   public void setCompanyName(String companyName) {
      this.companyName = companyName;
   }

   public String getRole() {
      return role;
   }

   public void setRole(String role) {
      this.role = role;
   }

   @Override
   public String toString() {
      return "[companyId=" + companyId + ", companyName=" + companyName + ", email=" + email + ", role=" + role
            + ", userId=" + userId + ", userName=" + userName + "]";
   }

}
