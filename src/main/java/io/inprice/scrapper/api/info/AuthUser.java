package io.inprice.scrapper.api.info;

import io.inprice.scrapper.common.meta.UserType;
import io.inprice.scrapper.common.models.Model;

public class AuthUser extends Model {

    private String email;
    private UserType type;
    private String fullName;
    private Long companyId;
    private Long workspaceId;

    public AuthUser() {
    }

    public AuthUser(Long id, String email, UserType type, String fullName, Long companyId, Long workspaceId) {
        this.email = email;
        this.type = type;
        this.fullName = fullName;
        this.companyId = companyId;
        this.workspaceId = workspaceId;
        setId(id);
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

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}
