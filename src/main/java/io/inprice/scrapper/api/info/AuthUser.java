package io.inprice.scrapper.api.info;

import java.io.Serializable;
import java.util.Map;

import io.inprice.scrapper.api.app.user.UserCompany;

public class AuthUser implements Serializable {

   private static final long serialVersionUID = 5763780271600754333L;

   private Long id;
   private String email;
   private String name;
   private Map<Long, UserCompany> companies;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

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

   public Map<Long, UserCompany> getCompanies() {
      return companies;
   }

   public void setCompanies(Map<Long, UserCompany> companies) {
      this.companies = companies;
   }

   @Override
   public String toString() {
      return "[id=" + id + ", email=" + email + ", name=" + name + "]";
   }

}
