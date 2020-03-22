package io.inprice.scrapper.api.app.product;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class Product implements Serializable {

   private static final long serialVersionUID = 2010109845985968128L;

   private Long id;
   private Boolean active = Boolean.TRUE;
   private String code;
   private String name;
   private String brand;
   private String category;
   private BigDecimal price;
   private Integer position;
   private String minSeller;
   private String maxSeller;
   private BigDecimal minPrice;
   private BigDecimal avgPrice;
   private BigDecimal maxPrice;
   private Long companyId;
   private Date updatedAt;
   private Date createdAt;

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

   public String getCode() {
      return code;
   }

   public void setCode(String code) {
      this.code = code;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getBrand() {
      return brand;
   }

   public void setBrand(String brand) {
      this.brand = brand;
   }

   public String getCategory() {
      return category;
   }

   public void setCategory(String category) {
      this.category = category;
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

}
