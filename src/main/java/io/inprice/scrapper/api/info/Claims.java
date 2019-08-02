package io.inprice.scrapper.api.info;

public class Claims {

    private long companyId;
    private long workspaceId;
    private long userId;

    public Claims() {
    }

    public Claims(long companyId, long workspaceId, long userId) {
        this.companyId = companyId;
        this.workspaceId = workspaceId;
        this.userId = userId;
    }

    public long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(long companyId) {
        this.companyId = companyId;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }
}
