package io.inprice.scrapper.api.app.link;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class LinkPrice implements Serializable {

   private static final long serialVersionUID = 6818942944451174569L;

   private Long id;
   private Long linkId;
   private BigDecimal price;
   private Long productId;
   private Long companyId;
   private Date createdAt;

   public LinkPrice() {
   }

   public LinkPrice(Long linkId, BigDecimal price) {
      this.linkId = linkId;
      this.price = price;
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getLinkId() {
      return linkId;
   }

   public void setLinkId(Long linkId) {
      this.linkId = linkId;
   }

   public BigDecimal getPrice() {
      return price;
   }

   public void setPrice(BigDecimal price) {
      this.price = price;
   }

   public Long getProductId() {
      return productId;
   }

   public void setProductId(Long productId) {
      this.productId = productId;
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

}
