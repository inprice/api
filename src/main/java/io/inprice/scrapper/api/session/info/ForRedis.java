package io.inprice.scrapper.api.session.info;

import java.util.Date;

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
  private String timezone;
  private String currencyFormat;
  private String hash;
  private Date accessedAt = new Date();

  public ForRedis(ForResponse forResponse, Long userId, Long companyId, String hash) {
    super(forResponse);
    this.userId = userId;
    this.companyId = companyId;
    this.hash = hash;
  }

}