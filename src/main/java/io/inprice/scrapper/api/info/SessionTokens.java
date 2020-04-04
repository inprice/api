package io.inprice.scrapper.api.info;

import java.io.Serializable;

public class SessionTokens implements Serializable {

   private static final long serialVersionUID = -6574420397041468461L;

   private String access;
   private String refresh;

   public SessionTokens(String access, String refresh) {
      this.access = access;
      this.refresh = refresh;
   }

   public SessionTokens() {
   }

   public String getAccess() {
      return access;
   }

   public void setAccess(String access) {
      this.access = access;
   }

   public String getRefresh() {
      return refresh;
   }

   public void setRefresh(String refresh) {
      this.refresh = refresh;
   }

}