package io.inprice.scrapper.api.models;

import java.util.Date;

public class CustomerPlan extends InfoModel {

    private Boolean monthly;
    private Date dueDate;
    private Date lastCollectingTime;
    private Boolean lastCollectingStatus;
    private Integer collectingRetries;
    private Long customerId;
    private Long brandId;
    private Long planId;
    private Date insertAt;

    public Boolean getMonthly() {
        return monthly;
    }

    public void setMonthly(Boolean monthly) {
        this.monthly = monthly;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getLastCollectingTime() {
        return lastCollectingTime;
    }

    public void setLastCollectingTime(Date lastCollectingTime) {
        this.lastCollectingTime = lastCollectingTime;
    }

    public Boolean getLastCollectingStatus() {
        return lastCollectingStatus;
    }

    public void setLastCollectingStatus(Boolean lastCollectingStatus) {
        this.lastCollectingStatus = lastCollectingStatus;
    }

    public Integer getCollectingRetries() {
        return collectingRetries;
    }

    public void setCollectingRetries(Integer collectingRetries) {
        this.collectingRetries = collectingRetries;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Date getInsertAt() {
        return insertAt;
    }

    public void setInsertAt(Date insertAt) {
        this.insertAt = insertAt;
    }

    @Override
    public String toString() {
        return "CustomerPlan{" +
                "active=" + getName() +
                ", name='" + getName() + '\'' +
                ", monthly=" + monthly +
                ", dueDate=" + dueDate +
                ", lastCollectingTime=" + lastCollectingTime +
                ", lastCollectingStatus=" + lastCollectingStatus +
                ", collectingRetries=" + collectingRetries +
                ", customerId=" + customerId +
                ", brandId=" + brandId +
                ", planId=" + planId +
                ", insertAt=" + insertAt +
                '}';
    }
}
