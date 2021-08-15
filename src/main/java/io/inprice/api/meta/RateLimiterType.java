package io.inprice.api.meta;

/**
 * RateLimiterType
 */
public enum RateLimiterType {

   REGISTER(RateLimiterType.FIVE_MINUTES),
   FORGOT_PASSWORD(RateLimiterType.FIVE_MINUTES),
   HANDLE_INVITATION(RateLimiterType.FIVE_MINUTES);

   private static final int FIVE_MINUTES = 5 * 60;

   private long TTL; // as seconds

   private RateLimiterType(long ttl) {
      this.TTL = ttl;
   }

   public long getTTL() {
      return this.TTL;
   }

}