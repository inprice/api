package io.inprice.scrapper.api.session.info;

import java.io.Serializable;

import io.inprice.scrapper.api.app.user.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ForResponse implements Serializable {

   private static final long serialVersionUID = -3414991620052194958L;

   private String user;
   private String email;
   private String company;
   private String timezone;
   private String currencyFormat;
   private UserRole role;

   public ForResponse(ForResponse forResponse) {
      this.user = forResponse.getUser();
      this.email = forResponse.getEmail();
      this.company = forResponse.getCompany();
      this.timezone = forResponse.getTimezone();
      this.currencyFormat = forResponse.getCurrencyFormat();
      this.role = forResponse.getRole();
   }

   public ForResponse(ForCookie forCookie, String user, 
    String company, String timezone, String currencyFormat) {
      this.user = user;
      this.email = forCookie.getEmail();
      this.company = company;
      this.timezone = timezone;
      this.currencyFormat = currencyFormat;
      this.role = forCookie.getRole();
   }

   public ForResponse(ForCookie forCookie, ForRedis forRedis) {  
      this.user = forRedis.getUser();
      this.email = forCookie.getEmail();
      this.company = forRedis.getCompany();
      this.timezone = forRedis.getTimezone();
      this.currencyFormat = forRedis.getCurrencyFormat();
      this.role = forCookie.getRole();
   }

  }