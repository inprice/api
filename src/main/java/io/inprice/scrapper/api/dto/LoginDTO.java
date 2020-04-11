package io.inprice.scrapper.api.dto;

public class LoginDTO extends PasswordDTO {

   private static final long serialVersionUID = 1L;

   private String email;

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   @Override
   public String toString() {
      return "LoginDTO [email=" + email + "]";
   }

}
