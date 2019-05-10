package io.inprice.scrapper.api.models;

import io.inprice.crawler.common.meta.LinkStatus;

import java.util.Date;

public class CustomerPlanHistory extends Model {

    private Long customerPlanId;
    private Boolean collectingStatus;
    private Date insertAt;

    public CustomerPlanHistory() {
    }

    public CustomerPlanHistory(Boolean collectingStatus) {
        this.collectingStatus = collectingStatus;
    }

    public CustomerPlanHistory(Long customerPlanId, Boolean collectingStatus) {
        this.customerPlanId = customerPlanId;
        this.collectingStatus = collectingStatus;
    }

    public Long getCustomerPlanId() {
        return customerPlanId;
    }

    public void setCustomerPlanId(Long customerPlanId) {
        this.customerPlanId = customerPlanId;
    }

    public Boolean getCollectingStatus() {
        return collectingStatus;
    }

    public void setCollectingStatus(Boolean collectingStatus) {
        this.collectingStatus = collectingStatus;
    }

    public Date getInsertAt() {
        return insertAt;
    }

    public void setInsertAt(Date insertAt) {
        this.insertAt = insertAt;
    }
}
