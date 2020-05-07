package io.inprice.scrapper.api.session.info;

import java.util.Date;

/**
 * Used in Redis
 */
public class ForRedis extends ForResponse {

  private static final long serialVersionUID = 3438172317056990343L;

  private Long userId;
  private Long companyId;
  private String timezone;
  private String currencyFormat;
  private String hash;
  private Date accessedAt = new Date();

  public ForRedis() {
  }

  public ForRedis(ForResponse forResponse, Long userId, Long companyId, String hash) {
    super(forResponse);
    this.userId = userId;
    this.companyId = companyId;
    this.hash = hash;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getTimezone() {
    return timezone;
  }

  public String getCurrencyFormat() {
    return currencyFormat;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public Date getAccessedAt() {
    return accessedAt;
  }

  public void setAccessedAt(Date accessedAt) {
    this.accessedAt = accessedAt;
  }

  @Override
  public String toString() {
    return "ForRedis [companyId=" + companyId + ", userId=" + userId + "]";
  }

}