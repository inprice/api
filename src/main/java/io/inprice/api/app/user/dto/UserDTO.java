package io.inprice.api.app.user.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class UserDTO extends PasswordDTO {

  private static final long serialVersionUID = -4510116778307627456L;

  private String email;
  private String name;
  private String timezone;
  private Long workspaceId;

}
