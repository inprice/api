package io.inprice.scrapper.api.info;

import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.Model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AuthUser extends Model {

    private String sessionId = UUID.randomUUID().toString();

    private String email;
    private UserType type;
    private String fullName;
    private Long companyId;
    private Set<Long> allowedWorkspaces = new HashSet<>();

    public String getSessionId() {
        return sessionId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserType getType() {
        return type;
    }

    public void setType(UserType type) {
        this.type = type;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Set<Long> getAllowedWorkspaces() {
        return allowedWorkspaces;
    }

    public void setAllowedWorkspaces(Set<Long> allowedWorkspaces) {
        this.allowedWorkspaces = allowedWorkspaces;
    }
}
