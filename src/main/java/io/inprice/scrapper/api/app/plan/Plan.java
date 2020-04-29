package io.inprice.scrapper.api.app.plan;

import java.io.Serializable;
import java.math.BigDecimal;

public class Plan implements Serializable {

   private static final long serialVersionUID = -4787008755878198572L;

   private String name;
   private String description;
   private BigDecimal price;
   private Integer rowLimit;
   private Integer userLimit;
   private Integer orderNo;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public BigDecimal getPrice() {
      return price;
   }

   public void setPrice(BigDecimal price) {
      this.price = price;
   }

   public Integer getRowLimit() {
      return rowLimit;
   }

   public void setRowLimit(Integer rowLimit) {
      this.rowLimit = rowLimit;
   }

   public Integer getUserLimit() {
      return userLimit;
   }

   public void setUserLimit(Integer userLimit) {
      this.userLimit = userLimit;
   }

   public Integer getOrderNo() {
      return orderNo;
   }

   public void setOrderNo(Integer orderNo) {
      this.orderNo = orderNo;
   }

}
