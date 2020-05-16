package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PasswordDTO implements Serializable {

   private static final long serialVersionUID = -326743507759919356L;

   private Long id;
   private String oldPassword;
   private String password;
   private String repeatPassword;
   private String token;

   public PasswordDTO() {
   }

   public PasswordDTO(Long id) {
      this.id = id;
   }

}
