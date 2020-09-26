package io.inprice.api.session.info;

import java.io.Serializable;
import java.util.Date;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ForDatabase implements Serializable {

   private static final long serialVersionUID = 5073154009606337304L;

   private String hash;
   private Long userId;
   private Long companyId;
   private String ip;
   private String os;
   private String browser;
   private String userAgent;
   private Date accessedAt = new Date();

   public ForDatabase(String hash, Long userId, Long companyId, String ip, String os, String browser,
       String userAgent) {
     this.hash = hash;
     this.userId = userId;
     this.companyId = companyId;
     this.ip = ip;
     this.os = os;
     this.browser = browser;
     this.userAgent = userAgent;
   }

}