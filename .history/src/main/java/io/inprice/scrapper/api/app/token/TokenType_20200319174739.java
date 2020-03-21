package io.inprice.scrapper.api.app.token;

public enum TokenType {

   ACCESS(15 * 60 * 1000), //15 minutes
   REFRESH(60 * 60 * 1000), //1 hours
   INVITATION_CONFIRM(3 * 24 * 60 * 60 * 1000), //3 days
   INVITATION_REJECT(3 * 24 * 60 * 60 * 1000); //3 days

   private long ttl; // as seconds

   private TokenType(long ttl) {
      this.ttl = ttl;
   }

   public long ttl() {
      return this.ttl;
   }

}
