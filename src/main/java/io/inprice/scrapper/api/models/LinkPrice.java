package io.inprice.scrapper.api.models;

import java.math.BigDecimal;
import java.util.Date;

public class LinkPrice extends Model {

    private Long linkId;
    private BigDecimal price;
    private String stockStatus;
    private Date insertAt;

    public LinkPrice() {
    }

    public LinkPrice(BigDecimal price, String stockStatus) {
        this.price = price;
        this.stockStatus = stockStatus;
    }

    public LinkPrice(Long linkId, BigDecimal price, String stockStatus) {
        this.linkId = linkId;
        this.price = price;
        this.stockStatus = stockStatus;
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

    public String getStockStatus() {
        return stockStatus;
    }

    public void setStockStatus(String stockStatus) {
        this.stockStatus = stockStatus;
    }

    public Date getInsertAt() {
        return insertAt;
    }

    public void setInsertAt(Date insertAt) {
        this.insertAt = insertAt;
    }
}
