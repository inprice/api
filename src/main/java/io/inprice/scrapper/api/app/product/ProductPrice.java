package io.inprice.scrapper.api.app.product;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class ProductPrice implements Serializable {

   private static final long serialVersionUID = 5103850978461831401L;

   private Long id;
   private Long productId;
   private String minSeller;
   private String maxSeller;
   private BigDecimal price;
   private Integer position;
   private BigDecimal minPrice;
   private BigDecimal avgPrice;
   private BigDecimal maxPrice;
   private Long companyId;
   private Date createdAt;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getProductId() {
      return productId;
   }

   public void setProductId(Long productId) {
      this.productId = productId;
   }

   public String getMinSeller() {
      return minSeller;
   }

   public void setMinSeller(String minSeller) {
      this.minSeller = minSeller;
   }

   public String getMaxSeller() {
      return maxSeller;
   }

   public void setMaxSeller(String maxSeller) {
      this.maxSeller = maxSeller;
   }

   public BigDecimal getPrice() {
      return price;
   }

   public void setPrice(BigDecimal price) {
      this.price = price;
   }

   public Integer getPosition() {
      return position;
   }

   public void setPosition(Integer position) {
      this.position = position;
   }

   public BigDecimal getMinPrice() {
      return minPrice;
   }

   public void setMinPrice(BigDecimal minPrice) {
      this.minPrice = minPrice;
   }

   public BigDecimal getAvgPrice() {
      return avgPrice;
   }

   public void setAvgPrice(BigDecimal avgPrice) {
      this.avgPrice = avgPrice;
   }

   public BigDecimal getMaxPrice() {
      return maxPrice;
   }

   public void setMaxPrice(BigDecimal maxPrice) {
      this.maxPrice = maxPrice;
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
