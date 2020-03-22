package io.inprice.scrapper.api.info;

import java.util.List;
import io.inprice.scrapper.api.consts.Responses;

public final class ServiceResponse {

   private int status;
   private String reason;
   private Object data;
   private List<String> problems;

   public ServiceResponse(String reason) {
      this.status = 0;
      this.reason = reason;
   }

   public ServiceResponse(int status, String reason) {
      this.status = status;
      this.reason = reason;
   }

   public ServiceResponse(Object data) {
      this.status = Responses.OK.getStatus();
      this.data = data;
   }

   public ServiceResponse(List<String> problems) {
      this.status = Responses.DataProblem.FORM_VALIDATION.getStatus();
      this.problems = problems;
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
