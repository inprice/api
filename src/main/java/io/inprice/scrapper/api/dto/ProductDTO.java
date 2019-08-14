package io.inprice.scrapper.api.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Used for handling product info in client side
 */
public class ProductDTO {

    private Long id;
    private String code;
    private String name;
    private String brand;
    private BigDecimal price;
    private Integer position;
    private String minSeller;
    private String maxSeller;
    private BigDecimal minPrice;
    private BigDecimal avgPrice;
    private BigDecimal maxPrice;
    private Date lastUpdate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public String toString() {
        return "ProductDTO{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", brand='" + brand + '\'' +
                ", price=" + price +
                ", position=" + position +
                ", minSeller='" + minSeller + '\'' +
                ", maxSeller='" + maxSeller + '\'' +
                ", minPrice=" + minPrice +
                ", avgPrice=" + avgPrice +
                ", maxPrice=" + maxPrice +
                ", lastUpdate=" + lastUpdate +
                '}';
    }
}
