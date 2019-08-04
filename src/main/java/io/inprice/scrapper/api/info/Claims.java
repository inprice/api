package io.inprice.scrapper.api.info;

import io.inprice.scrapper.common.meta.UserType;

public class Claims {

    private long companyId;
    private long workspaceId;
    private long userId;
    private UserType userType;

    public Claims() {
    }

    public Claims(long companyId, long workspaceId, long userId, UserType userType) {
        this.companyId = companyId;
        this.workspaceId = workspaceId;
        this.userId = userId;
        this.userType = userType;
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

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }
}
