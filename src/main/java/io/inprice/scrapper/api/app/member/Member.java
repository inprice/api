package io.inprice.scrapper.api.app.member;

import java.io.Serializable;
import java.util.Date;

import io.inprice.scrapper.api.app.user.UserRole;

public class Member implements Serializable {

   private static final long serialVersionUID = -7793356216234713734L;

   private Long id;
   private String email;
   private Long companyId;
   private UserRole role;
   private MemberStatus status = MemberStatus.PENDING;
   private MemberStatus preStatus = MemberStatus.PENDING;
   private Integer retry = 1;
   private Date createdAt = new Date();

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

   public MemberStatus getStatus() {
      return status;
   }

   public void setStatus(MemberStatus status) {
      this.status = status;
   }

   public MemberStatus getPreStatus() {
      return preStatus;
   }

   public void setPreStatus(MemberStatus preStatus) {
      this.preStatus = preStatus;
   }

   public Integer getRetry() {
      return retry;
   }

   public void setRetry(Integer retry) {
      this.retry = retry;
   }

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

}
