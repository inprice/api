package io.inprice.scrapper.api.app.token;

public enum TokenType {

   ACCESS(15 * TokenType.ONE_MINUTE),
   REFRESH(TokenType.ONE_HOUR),

   PASSWORD_RESET(3 * TokenType.ONE_HOUR),
   REGISTER_REQUEST(3 * TokenType.ONE_HOUR),

   INVITATION_CONFIRM(3 * TokenType.ONE_DAY),
   INVITATION_REJECT(3 * TokenType.ONE_DAY);

   private static final int ONE_MINUTE = 60 * 1000;
   private static final int ONE_HOUR = 60 * ONE_MINUTE;
   private static final int ONE_DAY = 24 * ONE_HOUR;

   private long ttl; // as seconds

   private TokenType(long ttl) {
      this.ttl = ttl;
   }

   public long ttl() {
      return this.ttl;
   }

}
