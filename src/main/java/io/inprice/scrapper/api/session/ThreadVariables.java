package io.inprice.scrapper.api.session;

class ThreadVariables {

   private SessionInToken sestok;
   private SessionInDB sesdb;

   ThreadVariables() {
   }

   void set(SessionInToken sestok, SessionInDB sesdb) {
      this.sestok = sestok;
      this.sesdb = sesdb;
   }

   SessionInToken getFromToken() {
      return sestok;
   }

   SessionInDB getFromDatabase() {
      return sesdb;
   }

}
