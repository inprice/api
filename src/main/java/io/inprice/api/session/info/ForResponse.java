package io.inprice.api.session.info;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import io.inprice.common.config.Plans;
import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Company;
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
  private Integer planId;
  private String planName;
  private String companyStatus;
  private Boolean everSubscribed;
  private Date subsRenewalAt;
  private Date lastStatusUpdate;
  private int daysToRenewal;
  private String currencyFormat;
  private String timezone;
  private Integer productCount;
  private UserRole role;

  public ForResponse(Company company, String user, String email, UserRole role, String timezone) {
    this.user = user;
    this.email = email;
    this.company = company.getName();
    this.planName = company.getPlanName();
    this.companyStatus = company.getStatus().name();
    this.lastStatusUpdate = company.getLastStatusUpdate();
    this.subsRenewalAt = company.getSubsRenewalAt();
    this.currencyFormat = company.getCurrencyFormat();
    this.timezone = timezone;
    this.role = role;
    this.productCount = company.getProductCount();

    this.everSubscribed = StringUtils.isNotBlank(company.getSubsCustomerId());
    doTheStandardAssignments();
  }

  public ForResponse(ForResponse forResponse) {
    this.user = forResponse.getUser();
    this.email = forResponse.getEmail();
    this.company = forResponse.getCompany();
    this.planName = forResponse.getPlanName();
    this.companyStatus = forResponse.getCompanyStatus();
    this.lastStatusUpdate = forResponse.getLastStatusUpdate();
    this.subsRenewalAt = forResponse.getSubsRenewalAt();
    this.currencyFormat = forResponse.getCurrencyFormat();
    this.timezone = forResponse.getTimezone();
    this.role = forResponse.getRole();
    this.productCount = forResponse.getProductCount();

    this.everSubscribed = forResponse.getEverSubscribed();
    doTheStandardAssignments();
  }

  public ForResponse(ForCookie forCookie, ForRedis forRedis) {
    this.user = forRedis.getUser();
    this.email = forCookie.getEmail();
    this.company = forRedis.getCompany();
    this.planName = forRedis.getPlanName();
    this.companyStatus = forRedis.getCompanyStatus();
    this.lastStatusUpdate = forRedis.getLastStatusUpdate();
    this.subsRenewalAt = forRedis.getSubsRenewalAt();
    this.currencyFormat = forRedis.getCurrencyFormat();
    this.timezone = forRedis.getTimezone();
    this.role = UserRole.valueOf(forCookie.getRole());
    this.productCount = forRedis.getProductCount();

    this.everSubscribed = forRedis.getEverSubscribed();
    doTheStandardAssignments();
  }

  public ForResponse(ForCookie forCookie, User user, Member mem) {
    this.user = user.getName();
    this.email = forCookie.getEmail();
    this.company = mem.getCompanyName();
    this.planName = mem.getPlanName();
    this.companyStatus = mem.getCompanyStatus().name();
    this.lastStatusUpdate = mem.getLastStatusUpdate();
    this.subsRenewalAt = mem.getSubsRenewalAt();
    this.currencyFormat = mem.getCurrencyFormat();
    this.timezone = user.getTimezone();
    this.role = UserRole.valueOf(forCookie.getRole());
    this.productCount = mem.getProductCount();

    this.everSubscribed = mem.getEverSubscribed();
    doTheStandardAssignments();
  }

  private void doTheStandardAssignments() {
    if (this.subsRenewalAt != null) {
      this.daysToRenewal = 1 + (int) DateUtils.findDayDiff(new Date(), this.subsRenewalAt);
    }
    if (this.planName != null) {
      this.planId = Plans.findByName(this.planName).getId();
    }

  }

}