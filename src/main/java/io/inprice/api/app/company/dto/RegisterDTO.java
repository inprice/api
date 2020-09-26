package io.inprice.api.app.company.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO implements Serializable {

  private static final long serialVersionUID = 7416774892611386665L;

  private String email;
  private String companyName;
  private String password;
  private String repeatPassword;

}
