package io.inprice.scrapper.api.dto;

import java.io.Serializable;

public class CreateCompanyDTO implements Serializable {

  private static final long serialVersionUID = -8983343002065096998L;

  private String name;
  private String currencyCode;
  private String currencyFormat;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public String getCurrencyFormat() {
    return currencyFormat;
  }

  public void setCurrencyFormat(String currencyFormat) {
    this.currencyFormat = currencyFormat;
  }

  @Override
  public String toString() {
    return "[currencyCode=" + currencyCode + ", currencyFormat=" + currencyFormat + ", name=" + name + "]";
  }

}
