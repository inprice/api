package io.inprice.scrapper.api.info;

import io.inprice.scrapper.common.meta.Role;
import io.inprice.scrapper.common.models.Model;

import java.util.UUID;

public class AuthUser extends Model {

    private String sessionId = UUID.randomUUID().toString();

    private String email;
    private Role role;
    private String fullName;
    private Long companyId;
    private Long workspaceId;

    public String getSessionId() {
        return sessionId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}
