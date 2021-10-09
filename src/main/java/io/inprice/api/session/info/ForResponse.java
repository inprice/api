package io.inprice.api.session.info;

import java.io.Serializable;
import java.util.Date;

import io.inprice.common.meta.UserRole;
import io.inprice.common.models.Workspace;
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

  private String fullName;
  private String email;
  private String workspace;
  private String workspaceStatus;
  private Integer planId;
  private String planName;
  private Integer linkCount;
  private Date subsStartedAt;
  private Date subsRenewalAt;
  private Date lastStatusUpdate;
  private String timezone;
  private String currencyFormat;
  private UserRole role;

  private Long workspaceId; //for only super users!
  
  public ForResponse(Workspace workspace, String fullName, String email, UserRole role, String timezone) {
    this.fullName = fullName;
    this.email = email;
    this.workspace = workspace.getName();
    this.workspaceStatus = workspace.getStatus().name();
    if (workspace.getPlan() != null) {
    	this.planId = workspace.getPlan().getId();
    	this.planName = workspace.getPlan().getName();
    }
    this.linkCount = workspace.getLinkCount();
    this.subsStartedAt = workspace.getSubsStartedAt();
    this.subsRenewalAt = workspace.getSubsRenewalAt();
    this.lastStatusUpdate = workspace.getLastStatusUpdate();
    this.timezone = timezone;
    this.currencyFormat = workspace.getCurrencyFormat();
    this.role = role;
  }

  public ForResponse(ForResponse forResponse) {
    this.fullName = forResponse.getFullName();
    this.email = forResponse.getEmail();
    this.workspace = forResponse.getWorkspace();
    this.workspaceStatus = forResponse.getWorkspaceStatus();
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
    this.fullName = forRedis.getFullName();
    this.email = forCookie.getEmail();
    this.workspace = forRedis.getWorkspace();
    this.workspaceStatus = forRedis.getWorkspaceStatus();
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
    this.fullName = user.getFullName();
    this.email = forCookie.getEmail();
    this.workspace = mem.getWorkspaceName();
    this.workspaceStatus = mem.getWorkspaceStatus().name();
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

  public ForResponse(Long workspaceId, String fullName, String email, String timezone) {
  	this.workspaceId = workspaceId;
    this.fullName = fullName;
    this.email = email;
    this.timezone = timezone;
  	this.role = UserRole.SUPER;
    this.workspace = "NOT SELECTED";
    this.workspaceStatus = "UNKNOWN";
    this.planId = 0;
    this.linkCount = 0;
  }

}