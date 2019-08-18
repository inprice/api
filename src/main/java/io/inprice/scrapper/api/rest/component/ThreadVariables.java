package io.inprice.scrapper.api.rest.component;

import io.inprice.scrapper.api.info.AuthUser;

class ThreadVariables {

    private long workspaceId;
    private AuthUser authUser;

    ThreadVariables() {
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public AuthUser getAuthUser() {
        return authUser;
    }

    void setAuthUser(AuthUser authUser) {
        this.authUser = authUser;
    }
}
