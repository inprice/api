package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.common.meta.UserType;

import java.io.Serializable;

/**
 * Used for handling user info in client side
 */
public class UserDTO extends PasswordDTO {

    private Boolean active;
    private UserType type;
    private String fullName;
    private String email;
    private Long companyId;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + getId() +
                ", active=" + active +
                ", type=" + type +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", passwordOld='" + getPasswordOld() + '\'' +
                ", password='" + getPassword() + '\'' +
                ", passwordAgain='" + getPasswordAgain() + '\'' +
                ", companyId=" + companyId +
                '}';
    }
}
