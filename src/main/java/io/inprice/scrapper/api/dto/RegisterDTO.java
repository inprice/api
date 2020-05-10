package io.inprice.scrapper.api.dto;

import java.io.Serializable;

/**
 * Used for handling company info from client side
 */
public class RegisterDTO implements Serializable {

  private static final long serialVersionUID = 7416774892611386665L;

  private String email;
  private String companyName;
  private String password;
  private String repeatPassword;

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
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

  @Override
  public String toString() {
    return "[companyName=" + companyName + ", email=" + email + "]";
  }

}
