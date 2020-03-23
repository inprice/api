package io.inprice.scrapper.api.framework;

/**
 * HandlerInterruptException
 */
public class HandlerInterruptException extends Exception {

   private static final long serialVersionUID = 7556915818040875668L;

   private int status;

   public HandlerInterruptException() {
      super();
   }

   public HandlerInterruptException(int status, String reason) {
      super(reason);
      this.status = status;
   }

   public int getStatus() {
      return status;
   }

}