package io.inprice.scrapper.api.meta;

/**
 * RateLimiterType
 */
public enum RateLimiterType {

   REGISTER(RateLimiterType.FIVE_MINUTES),
   FORGOT_PASSWORD(RateLimiterType.FIVE_MINUTES),
   HANDLE_INVITATION(RateLimiterType.FIVE_MINUTES);

   private static final int FIVE_MINUTES = 5 * 60 * 1000;

   private long ttl; // as seconds

   private RateLimiterType(long ttl) {
      this.ttl = ttl;
   }

   public long ttl() {
      return this.ttl;
   }

}