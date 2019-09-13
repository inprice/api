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

    public static Long getCompanyId() {
        return THREAD_VARIABLES.get().getAuthUser().getCompanyId();
    }

    public static Long getWorkspaceId() {
        return THREAD_VARIABLES.get().getWorkspaceId();
    }

    public static Long getUserId() {
        return THREAD_VARIABLES.get().getAuthUser().getId();
    }

    static void setAuthUser(AuthUser authUser) {
        THREAD_VARIABLES.get().setAuthUser(authUser);
    }
    static void setWorkspaceId(Long workspaceId) {
        THREAD_VARIABLES.get().setWorkspaceId(workspaceId);
    }

    public static void cleanup() {
        THREAD_VARIABLES.set(null);
        THREAD_VARIABLES.remove();
    }

}
