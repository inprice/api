package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.member.MemberStatus;

public class MemberChangeStatusDTO {

   private Long id;
   private MemberStatus status;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public MemberStatus getStatus() {
      return status;
   }

   public void setStatus(MemberStatus status) {
      this.status = status;
   }

   @Override
   public String toString() {
      return "MemberChangeStatusDTO [id=" + id + ", status=" + status + "]";
   }

}
