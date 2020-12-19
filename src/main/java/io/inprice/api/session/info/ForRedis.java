package io.inprice.api.session.info;

import java.util.Date;

import io.inprice.common.models.Member;
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
  private Long accountId;
  private String hash;
  private Date accessedAt = new Date();

  public ForRedis(ForResponse forResponse, Member mem, String hash) {
    super(forResponse);
    this.userId = mem.getUserId();
    this.accountId = mem.getAccountId();
    this.hash = hash;
  }

}