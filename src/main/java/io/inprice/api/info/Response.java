package io.inprice.api.info;

import java.util.List;
import io.inprice.api.consts.Responses;

public final class Response {

   private int status;
   private String reason;
   private Object data;
   private List<String> problems;

   public Response(String reason) {
      this.status = 400;
      this.reason = reason;
   }

   public Response(int status, String reason) {
      this.status = status;
      this.reason = reason;
   }

   public Response(Object data) {
      this.status = Responses.OK.getStatus();
      this.data = data;
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

   public List<String> getProblems() {
      return problems;
   }

}
