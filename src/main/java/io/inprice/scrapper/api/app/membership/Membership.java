package io.inprice.scrapper.api.app.membership;

import java.io.Serializable;
import java.util.Date;

import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;

public class Membership implements Serializable {

   private static final long serialVersionUID = -7793356216234713734L;

   private Long id;
   private String email;
   private Long userId;
   private Long companyId;
   private UserRole role;
   private UserStatus status = UserStatus.PENDING;
   private UserStatus preStatus = UserStatus.PENDING;
   private Integer retry = 1;
   private Date updatedAt;
   private Date createdAt = new Date();

   // transient
   private String companyName;

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

   public Long getUserId() {
      return userId;
   }

   public void setUserId(Long userId) {
      this.userId = userId;
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
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

   public UserStatus getPreStatus() {
      return preStatus;
   }

   public void setPreStatus(UserStatus preStatus) {
      this.preStatus = preStatus;
   }

   public Integer getRetry() {
      return retry;
   }

   public void setRetry(Integer retry) {
      this.retry = retry;
   }

   public Date getUpdatedAt() {
      return updatedAt;
   }

   public void setUpdatedAt(Date updatedAt) {
      this.updatedAt = updatedAt;
   }

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

   public String getCompanyName() {
      return companyName;
   }

   public void setCompanyName(String companyName) {
      this.companyName = companyName;
   }

}
