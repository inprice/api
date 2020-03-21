package io.inprice.scrapper.api.app.plan;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class Plan implements Serializable {

   private static final long serialVersionUID = -4787008755878198572L;

   private Long id;
   private Boolean active = Boolean.TRUE;
   private BigDecimal price;
   private Integer rowLimit;
   private Integer orderNo;
   private List<PlanRows> planRows;

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

   public Integer getOrderNo() {
      return orderNo;
   }

   public void setOrderNo(Integer orderNo) {
      this.orderNo = orderNo;
   }

   public List<PlanRows> getPlanRows() {
      return planRows;
   }

   public void setPlanRows(List<PlanRows> planRows) {
      this.planRows = planRows;
   }

}
