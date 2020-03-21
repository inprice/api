package io.inprice.scrapper.api.dto;

/**
 * Used for handling company info from client side
 */
public class RegisterDTO extends CompanyDTO {

   private static final long serialVersionUID = 7416774892611386665L;

   private Long userId;
   private String userName;
   private String email;
   private String password;
   private String repeatPassword;

   public Long getUserId() {
      return userId;
   }

   public void setUserId(Long userId) {
      this.userId = userId;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public String getRepeatPassword() {
      return repeatPassword;
   }

   public void setRepeatPassword(String repeatPassword) {
      this.repeatPassword = repeatPassword;
   }

}
