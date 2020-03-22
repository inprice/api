package io.inprice.scrapper.api.dto;

/**
 * Used for handling user info from client side
 */
public class UserDTO extends PasswordDTO {

   private static final long serialVersionUID = -4510116778307627456L;

   private String email;
   private String name;
   private Long companyId;

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

}
