package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.member.MemberRole;

public class MemberDTO {

   private String email;
   private MemberRole role;
   private TokenType tokenType;

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public MemberRole getRole() {
      return role;
   }

   public void setRole(MemberRole role) {
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
