package io.inprice.scrapper.api.meta;

/**
 * RateLimiterType
 */
public enum RateLimiterType {

   REGISTER_(RateLimiterType.FIVE_MINUTES),
   FORGOT_PASSWORD_(RateLimiterType.FIVE_MINUTES);

   private static final int FIVE_MINUTES = 5 * 60 * 1000;

   private long ttl; // as seconds

   private RateLimiterType(long ttl) {
      this.ttl = ttl;
   }

   public long ttl() {
      return this.ttl;
   }

}