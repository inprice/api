package io.inprice.scrapper.api.dto;

public class LoginDTO extends PasswordDTO {

   private static final long serialVersionUID = 1L;

   private String email;
   private String ip;
   private String userAgent;

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public String getIp() {
      return ip;
   }

   public void setIp(String ip) {
      this.ip = ip;
   }

   public String getUserAgent() {
      return userAgent;
   }

   public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
   }

   @Override
   public String toString() {
      return "LoginDTO [email=" + email + ", ip=" + ip + ", userAgent=" + userAgent + "]";
   }

}
