package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.session.info.ForRedis;

class ThreadVariables {

   private ForRedis session;

   ThreadVariables() {
   }

   void set(ForRedis session) {
      this.session = session;
   }

   ForRedis getSession() {
      return session;
   }

}
