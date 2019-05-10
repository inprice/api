package io.inprice.scrapper.api.models;

import java.math.BigDecimal;

public class Plan extends InfoModel {

    private String desc1;
    private String desc2;
    private String desc3;
    private Integer rowLimit;
    private BigDecimal price;
    private BigDecimal price1;
    private Integer orderNo;
    private String cssClass;
    private Boolean free;

    public String getDesc1() {
        return desc1;
    }

    public void setDesc1(String desc1) {
        this.desc1 = desc1;
    }

    public String getDesc2() {
        return desc2;
    }

    public void setDesc2(String desc2) {
        this.desc2 = desc2;
    }

    public String getDesc3() {
        return desc3;
    }

    public void setDesc3(String desc3) {
        this.desc3 = desc3;
    }

    public Integer getRowLimit() {
        return rowLimit;
    }

    public void setRowLimit(Integer rowLimit) {
        this.rowLimit = rowLimit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice1() {
        return price1;
    }

    public void setPrice1(BigDecimal price1) {
        this.price1 = price1;
    }

    public Integer getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Integer orderNo) {
        this.orderNo = orderNo;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public Boolean getFree() {
        return free;
    }

    public void setFree(Boolean free) {
        this.free = free;
    }

    @Override
    public String toString() {
        return "Plan{" +
                "active=" + getActive() +
                ", name='" + getName() + '\'' +
                ", desc1='" + desc1 + '\'' +
                ", desc2='" + desc2 + '\'' +
                ", desc3='" + desc3 + '\'' +
                ", rowLimit=" + rowLimit +
                ", price=" + price +
                ", price1=" + price1 +
                ", orderNo=" + orderNo +
                ", cssClass='" + cssClass + '\'' +
                ", free=" + free +
                '}';
    }
}
