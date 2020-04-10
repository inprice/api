package io.inprice.scrapper.api.dto;

import java.io.Serializable;

public class EmailDTO implements Serializable {

   private static final long serialVersionUID = 5884990803032543745L;

   private String email;
   private Long companyId;

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }

   @Override
   public String toString() {
      return "EmailDTO [email=" + email + "]";
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

}
