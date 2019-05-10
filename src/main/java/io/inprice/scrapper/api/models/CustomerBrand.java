package io.inprice.scrapper.api.models;

import java.util.Date;

public class CustomerBrand extends Model {

    private String name;
    private Long customerId;
    private Date insertAt;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Date getInsertAt() {
        return insertAt;
    }

    public void setInsertAt(Date insertAt) {
        this.insertAt = insertAt;
    }

    @Override
    public String toString() {
        return "CustomerBrand{" +
                "name='" + name + '\'' +
                ", customerId=" + customerId +
                ", insertAt=" + insertAt +
                '}';
    }
}
