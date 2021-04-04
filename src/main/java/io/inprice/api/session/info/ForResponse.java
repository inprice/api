package io.inprice.api.session.info;

import java.io.Serializable;
import java.util.Date;

import io.inprice.common.config.Plans;
import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Account;
import io.inprice.common.models.Member;
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
  private Integer planId;
  private String planName;
  private String accountStatus;
  private Date subsStartedAt;
  private Boolean everSubscribed;
  private Date renewalAt;
  private Date lastStatusUpdate;
  private String currencyFormat;
  private String timezone;
  private Integer linkLimit;
  private Integer linkCount;
  private UserRole role;

  public ForResponse(Account account, String user, String email, UserRole role, String timezone) {
    this.user = user;
    this.email = email;
    this.account = account.getName();
    this.planName = account.getPlanName();
    this.accountStatus = account.getStatus().name();
    this.subsStartedAt = account.getSubsStartedAt();
    this.lastStatusUpdate = account.getLastStatusUpdate();
    this.renewalAt = account.getRenewalAt();
    this.currencyFormat = account.getCurrencyFormat();
    this.timezone = timezone;
    this.role = role;
    this.linkLimit = account.getLinkLimit();
    this.linkCount = account.getLinkCount();

    makeTheStandardAssignments();
  }

  public ForResponse(ForResponse forResponse) {
    this.user = forResponse.getUser();
    this.email = forResponse.getEmail();
    this.account = forResponse.getAccount();
    this.planName = forResponse.getPlanName();
    this.accountStatus = forResponse.getAccountStatus();
    this.subsStartedAt = forResponse.getSubsStartedAt();
    this.lastStatusUpdate = forResponse.getLastStatusUpdate();
    this.renewalAt = forResponse.getRenewalAt();
    this.currencyFormat = forResponse.getCurrencyFormat();
    this.timezone = forResponse.getTimezone();
    this.role = forResponse.getRole();
    this.linkLimit = forResponse.getLinkLimit();
    this.linkCount = forResponse.getLinkCount();

    makeTheStandardAssignments();
  }

  public ForResponse(ForCookie forCookie, ForRedis forRedis) {
    this.user = forRedis.getUser();
    this.email = forCookie.getEmail();
    this.account = forRedis.getAccount();
    this.planName = forRedis.getPlanName();
    this.accountStatus = forRedis.getAccountStatus();
    this.subsStartedAt = forRedis.getSubsStartedAt();
    this.lastStatusUpdate = forRedis.getLastStatusUpdate();
    this.renewalAt = forRedis.getRenewalAt();
    this.currencyFormat = forRedis.getCurrencyFormat();
    this.timezone = forRedis.getTimezone();
    this.role = UserRole.valueOf(forCookie.getRole());
    this.linkLimit = forRedis.getLinkLimit();
    this.linkCount = forRedis.getLinkCount();

    makeTheStandardAssignments();
  }

  public ForResponse(ForCookie forCookie, User user, Member mem) {
    this.user = user.getName();
    this.email = forCookie.getEmail();
    this.account = mem.getAccountName();
    this.planName = mem.getPlanName();
    this.accountStatus = mem.getAccountStatus().name();
    this.subsStartedAt = mem.getSubsStartedAt();
    this.lastStatusUpdate = mem.getLastStatusUpdate();
    this.renewalAt = mem.getRenewalAt();
    this.currencyFormat = mem.getCurrencyFormat();
    this.timezone = user.getTimezone();
    this.role = UserRole.valueOf(forCookie.getRole());
    this.linkLimit = mem.getLinkLimit();
    this.linkCount = mem.getLinkCount();

    makeTheStandardAssignments();
  }

  private void makeTheStandardAssignments() {
    if (this.planName != null) {
      this.planId = Plans.findByName(this.planName).getId();
    }
    this.everSubscribed = (this.subsStartedAt != null);
  }

}