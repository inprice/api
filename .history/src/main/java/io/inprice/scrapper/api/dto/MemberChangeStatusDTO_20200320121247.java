package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.member.MemberStatus;

public class MemberChangeStatusDTO {

   private Long id;
   private MemberStatus status;
   private boolean paused;

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

   public boolean isPaused() {
      return paused;
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   @Override
   public String toString() {
      return "MemberChangeStatusDTO [id=" + id + ", status=" + status + "]";
   }

}
