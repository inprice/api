package io.inprice.scrapper.api.dto;

import java.io.Serializable;

/**
 * Used for accepting user invitation
 */
public class InvitationAcceptDTO implements Serializable {

  private static final long serialVersionUID = -7246512135937071388L;

  private String name;
  private String timezone;
  private String password;
  private String repeatPassword;
  private String token;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
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

}
