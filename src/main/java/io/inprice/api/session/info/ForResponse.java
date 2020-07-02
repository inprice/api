package io.inprice.api.session.info;

import java.io.Serializable;

import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;
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
  private Long planId;
  private String timezone;
  private String currencyFormat;
  private UserRole role;

  public ForResponse(ForResponse forResponse) {
    this.user = forResponse.getUser();
    this.email = forResponse.getEmail();
    this.company = forResponse.getCompany();
    this.planId = forResponse.getPlanId();
    this.timezone = forResponse.getTimezone();
    this.currencyFormat = forResponse.getCurrencyFormat();
    this.role = forResponse.getRole();
  }

  public ForResponse(ForCookie forCookie, ForRedis forRedis) {
    this.user = forRedis.getUser();
    this.email = forCookie.getEmail();
    this.company = forRedis.getCompany();
    this.planId = forRedis.getPlanId();
    this.timezone = forRedis.getTimezone();
    this.currencyFormat = forRedis.getCurrencyFormat();
    this.role = UserRole.valueOf(forCookie.getRole());
  }

  public ForResponse(ForCookie forCookie, User user, Membership mem) {
    this.user = user.getName();
    this.email = forCookie.getEmail();
    this.company = mem.getCompanyName();
    this.planId = mem.getPlanId();
    this.timezone = user.getTimezone();
    this.currencyFormat = mem.getCurrencyFormat();
    this.role = UserRole.valueOf(forCookie.getRole());
  }

}