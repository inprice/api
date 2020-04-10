package io.inprice.scrapper.api.info;

import java.io.Serializable;

public class SessionTokens implements Serializable {

   private static final long serialVersionUID = -6574420397041468461L;

   private String access;
   private String refresh;
   private String ip;
   private String userAgent;
   private Long companyId;

   public SessionTokens() {
   }

   public SessionTokens(String access, String refresh, String ip, String userAgent, Long companyId) {
      this.access = access;
      this.refresh = refresh;
      this.ip = ip;
      this.userAgent = userAgent;
      this.companyId = companyId;
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

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

}