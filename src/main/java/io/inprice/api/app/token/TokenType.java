package io.inprice.api.app.token;

public enum TokenType {

  ACCESS(20 * TokenType.ONE_MINUTE),
  REFRESH(TokenType.ONE_HOUR),

  FORGOT_PASSWORD(3 * TokenType.ONE_HOUR),
  REGISTER_REQUEST(3 * TokenType.ONE_HOUR),

  INVITATION(3 * TokenType.ONE_DAY);

  private static final int ONE_SECOND = 1000;
  private static final int ONE_MINUTE = 60 * ONE_SECOND;
  private static final int ONE_HOUR = 60 * ONE_MINUTE;
  private static final int ONE_DAY = 24 * ONE_HOUR;

  private long ttl; // as milliseconds

  private TokenType(long ttl) {
    this.ttl = ttl;
  }

  public long ttl() {
    return this.ttl;
  }

}
