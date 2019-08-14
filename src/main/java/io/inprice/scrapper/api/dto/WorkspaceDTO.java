package io.inprice.scrapper.api.dto;

/**
 * Used for handling workspace info in client side
 */
public class WorkspaceDTO {

    private Long id;
    private String name;
    private Long planId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }
}
