package io.inprice.api.session.info;

import java.io.Serializable;
import java.util.Date;

import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Member;
import io.inprice.common.models.User;
import io.inprice.common.utils.DateUtils;
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
  private Boolean freeUsage;
  private String planName;
  private String subsStatus;
  private Date subsRenewalAt;
  private Integer daysToRenewal;
  private String currencyFormat;
  private String timezone;
  private Integer productCount;
  private UserRole role;

  public ForResponse(ForResponse forResponse) {
    this.user = forResponse.getUser();
    this.email = forResponse.getEmail();
    this.company = forResponse.getCompany();
    this.freeUsage = forResponse.getFreeUsage();
    this.planName = forResponse.getPlanName();
    this.subsStatus = forResponse.getSubsStatus();
    this.subsRenewalAt = forResponse.getSubsRenewalAt();
    this.currencyFormat = forResponse.getCurrencyFormat();
    this.timezone = forResponse.getTimezone();
    this.productCount = forResponse.getProductCount();
    this.role = forResponse.getRole();
    if (this.subsRenewalAt != null) {
      this.daysToRenewal = (int) DateUtils.findDayDiff(this.subsRenewalAt, new Date());
    }
  }

  public ForResponse(ForCookie forCookie, ForRedis forRedis) {
    this.user = forRedis.getUser();
    this.email = forCookie.getEmail();
    this.company = forRedis.getCompany();
    this.freeUsage = forRedis.getFreeUsage();
    this.planName = forRedis.getPlanName();
    this.subsStatus = forRedis.getSubsStatus();
    this.subsRenewalAt = forRedis.getSubsRenewalAt();
    this.currencyFormat = forRedis.getCurrencyFormat();
    this.timezone = forRedis.getTimezone();
    this.role = UserRole.valueOf(forCookie.getRole());
    this.productCount = forRedis.getProductCount();
    if (this.subsRenewalAt != null) {
      this.daysToRenewal = (int) DateUtils.findDayDiff(this.subsRenewalAt, new Date());
    }
  }

  public ForResponse(ForCookie forCookie, User user, Member mem) {
    this.user = user.getName();
    this.email = forCookie.getEmail();
    this.company = mem.getCompanyName();
    this.freeUsage = mem.getFreeUsage();
    this.planName = mem.getPlanName();
    this.subsStatus = mem.getSubsStatus();
    this.subsRenewalAt = mem.getSubsRenewalAt();
    this.currencyFormat = mem.getCurrencyFormat();
    this.timezone = user.getTimezone();
    this.role = UserRole.valueOf(forCookie.getRole());
    this.productCount = mem.getProductCount();
    if (this.subsRenewalAt != null) {
      this.daysToRenewal = (int) DateUtils.findDayDiff(this.subsRenewalAt, new Date());
    }
  }

}