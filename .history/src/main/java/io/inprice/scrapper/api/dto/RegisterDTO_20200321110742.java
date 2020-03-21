package io.inprice.scrapper.api.dto;

import java.io.Serializable;

/**
 * Used for handling company info from client side
 */
public class CompanyDTO implements Serializable {

   private static final long serialVersionUID = -8522304173530280287L;

   private String name;
   private String country;

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getCountry() {
      return country;
   }

   public void setCountry(String country) {
      this.country = country;
   }

}
