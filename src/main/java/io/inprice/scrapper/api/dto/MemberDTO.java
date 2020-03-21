package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.UserRole;

public class MemberDTO {

   private String email;
   private UserRole role;
   private TokenType tokenType;

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public UserRole getRole() {
      return role;
   }

   public void setRole(UserRole role) {
      this.role = role;
   }

   public TokenType getTokenType() {
      return tokenType;
   }

   public void setTokenType(TokenType tokenType) {
      this.tokenType = tokenType;
   }

   @Override
   public String toString() {
      return "MemberDTO [email=" + email + ", role=" + role + ", tokenType=" + tokenType + "]";
   }

}
