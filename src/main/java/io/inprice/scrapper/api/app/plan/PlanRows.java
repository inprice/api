package io.inprice.scrapper.api.app.plan;

import java.io.Serializable;

public class PlanRows implements Serializable {

   private static final long serialVersionUID = -3291041523633309400L;

   private Long id;
   private String description;
   private Integer orderNo;
   private Long planId;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public Integer getOrderNo() {
      return orderNo;
   }

   public void setOrderNo(Integer orderNo) {
      this.orderNo = orderNo;
   }

   public Long getPlanId() {
      return planId;
   }

   public void setPlanId(Long planId) {
      this.planId = planId;
   }

}
