package io.inprice.api.dto;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

   @Override
   public String toString() {
     return "[id=" + id + ", token=" + token + "]";
   }

}
