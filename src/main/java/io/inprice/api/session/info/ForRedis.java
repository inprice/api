package io.inprice.api.session.info;

import java.util.Date;

import io.inprice.common.meta.SubsStatus;
import io.inprice.common.models.Membership;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ForRedis extends ForResponse {

  private static final long serialVersionUID = 3438172317056990343L;

  private Long userId;
  private Long companyId;
  private Integer planId;
  private SubsStatus subsStatus;
  private Date subsRenewalAt;
  private String timezone;
  private String currencyFormat;
  private String hash;
  private Date accessedAt = new Date();

  public ForRedis(ForResponse forResponse, Membership mem, String hash) {
    super(forResponse);
    this.userId = mem.getUserId();
    this.companyId = mem.getCompanyId();
    this.planId = mem.getPlanId();
    this.subsStatus = mem.getSubsStatus();
    this.subsRenewalAt = mem.getSubsRenewalAt();
    this.hash = hash;
  }

}