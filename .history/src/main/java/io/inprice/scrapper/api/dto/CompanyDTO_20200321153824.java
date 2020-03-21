package io.inprice.scrapper.api.dto;

import java.io.Serializable;

/**
 * Used for handling company info from client side
 */
public class CompanyDTO implements Serializable {

   private String companyName;
   private String website;
   private String sector;
   private String country;

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
