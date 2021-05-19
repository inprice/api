package io.inprice.api.info;

import io.inprice.api.consts.Responses;

public final class Response {

   private int status;
   private String reason;
   private Object data;

   public Response(Object data) {
      this.status = Responses.OK.getStatus();
      this.data = data;
   }

   public Response(String reason) {
  	 this.status = Responses.BAD_REQUEST.getStatus();
      this.reason = reason;
   }

   public Response(int status, String reason) {
      this.status = status;
      this.reason = reason;
   }

   public boolean isOK() {
      return (status == Responses.OK.getStatus());
   }

   public int getStatus() {
      return status;
   }

   public String getReason() {
      return reason;
   }

   @SuppressWarnings("unchecked")
   public <T> T getData() {
      return (T) data;
   }

}
