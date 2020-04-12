package io.inprice.scrapper.api.app.auth;

import java.io.Serializable;
import java.util.Date;

public class UserSession implements Serializable {

   private static final long serialVersionUID = 5073154009606337304L;

   private String token;
   private Long userId;
   private Long companyId;
   private String ip;
   private String os;
   private String browser;
   private String userAgent;
   private Date accessedAt = new Date();

   public String getToken() {
      return token;
   }

   public void setToken(String token) {
      this.token = token;
   }

   public Long getUserId() {
      return userId;
   }

   public void setUserId(Long userId) {
      this.userId = userId;
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

   public String getIp() {
      return ip;
   }

   public void setIp(String ip) {
      this.ip = ip;
   }

   public String getOs() {
      return os;
   }

   public void setOs(String os) {
      this.os = os;
   }

   public String getBrowser() {
      return browser;
   }

   public void setBrowser(String browser) {
      this.browser = browser;
   }

   public String getUserAgent() {
      return userAgent;
   }

   public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
   }

   public Date getAccessedAt() {
      return accessedAt;
   }

   public void setAccessedAt(Date accessedAt) {
      this.accessedAt = accessedAt;
   }

   @Override
   public String toString() {
      return "[accessedAt=" + accessedAt + ", browser=" + browser + ", companyId=" + companyId + ", ip="
            + ip + ", os=" + os + ", userAgent=" + userAgent + ", userId=" + userId + "]";
   }

}