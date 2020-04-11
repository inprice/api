package io.inprice.scrapper.api.app.member;

import java.io.Serializable;
import java.util.Date;

public class Member implements Serializable {

   private static final long serialVersionUID = -7793356216234713734L;

   private Long id;
   private Boolean active;
   private String email;
   private Long userId;
   private Long companyId;
   private MemberRole role;
   private MemberStatus status = MemberStatus.PENDING;
   private MemberStatus preStatus = MemberStatus.PENDING;
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

   public Boolean getActive() {
      return active;
   }

   public void setActive(Boolean active) {
      this.active = active;
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

   public MemberRole getRole() {
      return role;
   }

   public void setRole(MemberRole role) {
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
