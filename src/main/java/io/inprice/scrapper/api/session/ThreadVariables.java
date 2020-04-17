package io.inprice.scrapper.api.session;

import io.inprice.scrapper.api.app.auth.SessionInfoForDB;
import io.inprice.scrapper.api.app.auth.SessionInfoForToken;

class ThreadVariables {

   private SessionInfoForToken sestok;
   private SessionInfoForDB sesdb;

   ThreadVariables() {
   }

   void set(SessionInfoForToken sestok, SessionInfoForDB sesdb) {
      this.sestok = sestok;
      this.sesdb = sesdb;
   }

   SessionInfoForToken getFromToken() {
      return sestok;
   }

   SessionInfoForDB getFromDatabase() {
      return sesdb;
   }

}
