package io.inprice.scrapper.api.app.user;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class User implements Serializable {

  private static final long serialVersionUID = 1753526228909974777L;

  private Long id;
  private String email;
  private String name;
  private String timezone;
  private String passwordHash;
  private String passwordSalt;
  private Date createdAt;

}
