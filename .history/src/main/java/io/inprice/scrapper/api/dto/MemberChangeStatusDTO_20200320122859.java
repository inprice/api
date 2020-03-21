package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.member.MemberStatus;

public class MemberChangeStatusDTO {

   private Long memberId;
   private MemberStatus status;
   private boolean paused;

   public Long getMemberId() {
      return memberId;
   }

   public void setMemberId(Long memberId) {
      this.memberId = memberId;
   }

   public MemberStatus getStatus() {
      return status;
   }

   public void setStatus(MemberStatus status) {
      this.status = status;
   }

   public boolean isPaused() {
      return paused;
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   @Override
   public String toString() {
      return "MemberChangeStatusDTO [memberId=" + memberId + ", paused=" + paused + ", status=" + status + "]";
   }

}
