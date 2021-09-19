package io.inprice.api.app.workspace.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterDTO implements Serializable {

  private static final long serialVersionUID = 7416774892611386665L;

  private String email;
  private String workspaceName;
  private String password;
  private String repeatPassword;

}
