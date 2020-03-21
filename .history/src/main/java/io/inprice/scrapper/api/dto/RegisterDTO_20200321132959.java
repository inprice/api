package io.inprice.scrapper.api.dto;

/**
 * Used for handling company info from client side
 */
public class RegisterDTO extends PasswordDTO {

   private static final long serialVersionUID = -8522304173530280287L;

   private Long userId;
   private String userName;
   private String email;
   private String companyName;
   private String website;
   private String sector;
   private String country;

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

   public String getCompanyName() {
      return companyName;
   }

   public void setCompanyName(String companyName) {
      this.companyName = companyName;
   }

   public String getWebsite() {
      return website;
   }

   public void setWebsite(String website) {
      this.website = website;
   }

   public String getSector() {
      return sector;
   }

   public void setSector(String sector) {
      this.sector = sector;
   }

   public String getCountry() {
      return country;
   }

   public void setCountry(String country) {
      this.country = country;
   }

}
