package io.inprice.scrapper.api.rest.component;

import io.inprice.scrapper.api.info.AuthUser;

class ThreadVariables {

    private AuthUser authUser;

    ThreadVariables() {
    }

    public AuthUser getAuthUser() {
        return authUser;
    }

    void setAuthUser(AuthUser authUser) {
        this.authUser = authUser;
    }
}
