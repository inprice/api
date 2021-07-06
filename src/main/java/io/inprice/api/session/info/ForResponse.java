package io.inprice.api.session.info;

import java.io.Serializable;
import java.util.Date;

import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Account;
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
  private String account;
  private String accountStatus;
  private Integer planId;
  private String planName;
  private Integer linkCount;
  private Date subsStartedAt;
  private Date subsRenewalAt;
  private Date lastStatusUpdate;
  private String timezone;
  private String currencyFormat;
  private UserRole role;

  private Long accountId; //for only super users!
  
  public ForResponse(Account account, String user, String email, UserRole role, String timezone) {
    this.user = user;
    this.email = email;
    this.account = account.getName();
    this.accountStatus = account.getStatus().name();
    if (account.getPlan() != null) {
    	this.planId = account.getPlan().getId();
    	this.planName = account.getPlan().getName();
    }
    this.linkCount = account.getLinkCount();
    this.subsStartedAt = account.getSubsStartedAt();
    this.subsRenewalAt = account.getSubsRenewalAt();
    this.lastStatusUpdate = account.getLastStatusUpdate();
    this.timezone = timezone;
    this.currencyFormat = account.getCurrencyFormat();
    this.role = role;
  }

  public ForResponse(ForResponse forResponse) {
    this.user = forResponse.getUser();
    this.email = forResponse.getEmail();
    this.account = forResponse.getAccount();
    this.accountStatus = forResponse.getAccountStatus();
    this.planId = forResponse.getPlanId();
    this.planName = forResponse.getPlanName();
    this.linkCount = forResponse.getLinkCount();
    this.subsStartedAt = forResponse.getSubsStartedAt();
    this.subsRenewalAt = forResponse.getSubsRenewalAt();
    this.lastStatusUpdate = forResponse.getLastStatusUpdate();
    this.timezone = forResponse.getTimezone();
    this.currencyFormat = forResponse.getCurrencyFormat();
    this.role = forResponse.getRole();
  }

  public ForResponse(ForCookie forCookie, ForRedis forRedis) {
    this.user = forRedis.getUser();
    this.email = forCookie.getEmail();
    this.account = forRedis.getAccount();
    this.accountStatus = forRedis.getAccountStatus();
    this.planId = forRedis.getPlanId();
    this.planName = forRedis.getPlanName();
    this.linkCount = forRedis.getLinkCount();
    this.subsStartedAt = forRedis.getSubsStartedAt();
    this.subsRenewalAt = forRedis.getSubsRenewalAt();
    this.lastStatusUpdate = forRedis.getLastStatusUpdate();
    this.timezone = forRedis.getTimezone();
    this.currencyFormat = forRedis.getCurrencyFormat();
    this.role = UserRole.valueOf(forCookie.getRole());
  }

  public ForResponse(ForCookie forCookie, User user, Membership mem) {
    this.user = user.getName();
    this.email = forCookie.getEmail();
    this.account = mem.getAccountName();
    this.accountStatus = mem.getAccountStatus().name();
    this.planId = mem.getPlanId();
    this.planName = mem.getPlanName();
    this.linkCount = mem.getLinkCount();
    this.subsStartedAt = mem.getSubsStartedAt();
    this.subsRenewalAt = mem.getSubsRenewalAt();
    this.lastStatusUpdate = mem.getLastStatusUpdate();
    this.timezone = user.getTimezone();
    this.currencyFormat = mem.getCurrencyFormat();
    this.role = UserRole.valueOf(forCookie.getRole());
  }

  public ForResponse(Long accountId, String username, String email, String timezone) {
  	this.accountId = accountId;
    this.user = username;
    this.email = email;
    this.timezone = timezone;
  	this.role = UserRole.SUPER;
    this.account = "NOT SELECTED";
    this.accountStatus = "UNKNOWN";
    this.planId = 0;
    this.linkCount = 0;
  }

}