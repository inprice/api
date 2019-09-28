package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.common.meta.UserType;

/**
 * Used for handling user info from client side
 */
public class UserDTO extends PasswordDTO {

    private UserType type;
    private String fullName;
    private String email;

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

    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + getId() +
                ", type=" + type +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
