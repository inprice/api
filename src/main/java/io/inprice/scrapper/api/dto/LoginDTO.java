package io.inprice.scrapper.api.dto;

public class LoginDTO extends PasswordDTO {

    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
