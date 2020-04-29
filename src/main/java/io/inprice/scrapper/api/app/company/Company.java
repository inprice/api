package io.inprice.scrapper.api.app.company;

import java.io.Serializable;
import java.util.Date;

import io.inprice.scrapper.api.app.plan.PlanStatus;

public class Company implements Serializable {

   private static final long serialVersionUID = 1818360516258349831L;

   private Long id;
   private String name;
   private String sector;
   private String website;
   private String country;
   private Long adminId;
   private Integer planId;
   private PlanStatus planStatus;
   private Date dueDate;
   private Integer retry = 0;
   private Date lastCollectingTime;
   private Boolean lastCollectingStatus = Boolean.FALSE;
   private Date createdAt = new Date();

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getSector() {
      return sector;
   }

   public void setSector(String sector) {
      this.sector = sector;
   }

   public String getWebsite() {
      return website;
   }

   public void setWebsite(String website) {
      this.website = website;
   }

   public String getCountry() {
      return country;
   }

   public void setCountry(String country) {
      this.country = country;
   }

   public Long getAdminId() {
      return adminId;
   }

   public void setAdminId(Long adminId) {
      this.adminId = adminId;
   }

   public Integer getPlanId() {
      return planId;
   }

   public void setPlanId(Integer planId) {
      this.planId = planId;
   }

   public PlanStatus getPlanStatus() {
      return planStatus;
   }

   public void setPlanStatus(PlanStatus planStatus) {
      this.planStatus = planStatus;
   }

   public Date getDueDate() {
      return dueDate;
   }

   public void setDueDate(Date dueDate) {
      this.dueDate = dueDate;
   }

   public Integer getRetry() {
      return retry;
   }

   public void setRetry(Integer retry) {
      this.retry = retry;
   }

   public Date getLastCollectingTime() {
      return lastCollectingTime;
   }

   public void setLastCollectingTime(Date lastCollectingTime) {
      this.lastCollectingTime = lastCollectingTime;
   }

   public Boolean getLastCollectingStatus() {
      return lastCollectingStatus;
   }

   public void setLastCollectingStatus(Boolean lastCollectingStatus) {
      this.lastCollectingStatus = lastCollectingStatus;
   }

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

}
