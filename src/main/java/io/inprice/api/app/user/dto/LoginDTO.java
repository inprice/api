package io.inprice.api.app.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO extends PasswordDTO {

  private static final long serialVersionUID = 2382878650396743157L;

  private String email;

}
