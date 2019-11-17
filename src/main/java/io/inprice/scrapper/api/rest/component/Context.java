package io.inprice.scrapper.api.rest.component;


import io.inprice.scrapper.api.info.AuthUser;

public class Context {

    private final static ThreadLocal<ThreadVariables> THREAD_VARIABLES = new ThreadLocal<ThreadVariables>() {
        @Override
        protected ThreadVariables initialValue() {
            return new ThreadVariables();
        }
    };

    public static AuthUser getAuthUser() {
        return THREAD_VARIABLES.get().getAuthUser();
    }

    static void setAuthUser(AuthUser authUser) {
        THREAD_VARIABLES.get().setAuthUser(authUser);
    }

    public static Long getUserId() {
        return THREAD_VARIABLES.get().getAuthUser().getId();
    }

    public static Long getWorkspaceId() {
        return THREAD_VARIABLES.get().getAuthUser().getWorkspaceId();
    }

    public static Long getCompanyId() {
        return THREAD_VARIABLES.get().getAuthUser().getCompanyId();
    }

    public static void cleanup() {
        THREAD_VARIABLES.set(null);
        THREAD_VARIABLES.remove();
    }

}
