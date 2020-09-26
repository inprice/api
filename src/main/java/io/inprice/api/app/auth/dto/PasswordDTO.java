package io.inprice.api.app.auth.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordDTO implements Serializable {

  private static final long serialVersionUID = -326743507759919356L;

  private Long id;
  private String oldPassword;
  private String password;
  private String repeatPassword;
  private String token;

  public PasswordDTO(Long id) {
    this.id = id;
  }

}
