package io.inprice.scrapper.api.dto;

import java.io.Serializable;

/**
 * Used for handling user password info
 */
public class PasswordDTO implements Serializable {

	private static final long serialVersionUID = 1L;

    private Long id;
    private String passwordOld;
    private String password;
    private String repeatPassword;
    private String token;

    public PasswordDTO() {
    }

    public PasswordDTO(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPasswordOld() {
        return passwordOld;
    }

    public void setPasswordOld(String passwordOld) {
        this.passwordOld = passwordOld;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRepeatPassword() {
        return repeatPassword;
    }

    public void setRepeatPassword(String repeatPassword) {
        this.repeatPassword = repeatPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "PasswordDTO{" +
                "id=" + id +
                ", passwordOld='" + passwordOld + '\'' +
                ", password='" + password + '\'' +
                ", repeatPassword='" + repeatPassword + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
