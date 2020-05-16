package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class InvitationAcceptDTO implements Serializable {

  private static final long serialVersionUID = -7246512135937071388L;

  private String token;
  private String password;
  private String repeatPassword;

}
