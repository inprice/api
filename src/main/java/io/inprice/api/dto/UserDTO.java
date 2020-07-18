package io.inprice.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO extends PasswordDTO {

  private static final long serialVersionUID = -4510116778307627456L;

  private String email;
  private String name;
  private String timezone;
  private Long companyId;

  @Override
  public String toString() {
    return "[id=" + getId() + ", companyId=" + companyId + ", email=" + email + ", name=" + name + ", timezone=" + timezone + "]";
  }

}
