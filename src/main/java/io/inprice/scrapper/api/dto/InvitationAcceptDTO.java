package io.inprice.scrapper.api.dto;

import java.io.Serializable;

/**
 * Used for accepting user invitation
 */
public class InvitationAcceptDTO implements Serializable {

  private static final long serialVersionUID = -7246512135937071388L;

  private String token;
  private String password;
  private String repeatPassword;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
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

}
