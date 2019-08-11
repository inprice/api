package io.inprice.scrapper.api.dto;

import java.util.Date;

/**
 * Used for handling workspace info in client side
 */
public class WorkspaceDTO {

    private Long id;
    private Boolean active;
    private String name;
    private Date dueDate;
    private Date lastCollectingTime;
    private Boolean lastCollectingStatus;
    private Integer retry;
    private Long planId;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Integer getRetry() {
        return retry;
    }

    public void setRetry(Integer retry) {
        this.retry = retry;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }
}
