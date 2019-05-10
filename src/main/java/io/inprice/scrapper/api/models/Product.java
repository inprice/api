package io.inprice.scrapper.api.models;

import java.math.BigDecimal;
import java.util.Date;

public class Product extends InfoModel {

    private String code;
    private String brand;
    private String category;
    private BigDecimal price;
    private Integer position;
    private Date lastUpdate;
    private String minSeller;
    private String maxSeller;
    private BigDecimal minPrice;
    private BigDecimal avgPrice;
    private BigDecimal maxPrice;
    private Long customerPlanId;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
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

    public Long getCustomerPlanId() {
        return customerPlanId;
    }

    public void setCustomerPlanId(Long customerPlanId) {
        this.customerPlanId = customerPlanId;
    }

    @Override
    public String toString() {
        return "Product{" +
                "code='" + code + '\'' +
                ", active='" + getActive() + '\'' +
                ", name='" + getName() + '\'' +
                ", brand='" + brand + '\'' +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", position=" + position +
                ", lastUpdate=" + lastUpdate +
                ", minSeller='" + minSeller + '\'' +
                ", maxSeller='" + maxSeller + '\'' +
                ", minPrice=" + minPrice +
                ", avgPrice=" + avgPrice +
                ", maxPrice=" + maxPrice +
                ", customerPlanId=" + customerPlanId +
                '}';
    }
}
