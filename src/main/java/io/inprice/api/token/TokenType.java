package io.inprice.api.token;

public enum TokenType {

  ACCESS(20 * TokenType.ONE_MINUTE),
  REFRESH(TokenType.ONE_HOUR),

  FORGOT_PASSWORD(10 * TokenType.ONE_MINUTE),
  REGISTRATION_REQUEST(10 * TokenType.ONE_MINUTE),

  INVITATION(3 * TokenType.ONE_DAY);

  private static final int ONE_SECOND = 1;
  private static final int ONE_MINUTE = 60 * ONE_SECOND;
  private static final int ONE_HOUR = 60 * ONE_MINUTE;
  private static final int ONE_DAY = 24 * ONE_HOUR;

  private long TTL; // as second

  private TokenType(long ttl) {
    this.TTL = ttl;
  }

  public long getTTL() {
    return this.TTL;
  }

}
