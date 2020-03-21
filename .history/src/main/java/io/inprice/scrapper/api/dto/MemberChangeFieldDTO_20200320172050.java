package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.member.MemberStatus;
import io.inprice.scrapper.api.app.user.UserRole;

public class MemberChangeFieldDTO {

   private boolean isStatusChange;
   private Long memberId;
   private UserRole role;
   private MemberStatus status;
   private boolean paused;

   public boolean isStatusChange() {
      return isStatusChange;
   }

   public void setStatusChange(boolean isStatusChange) {
      this.isStatusChange = isStatusChange;
   }

   public Long getMemberId() {
      return memberId;
   }

   public void setMemberId(Long memberId) {
      this.memberId = memberId;
   }

   public UserRole getRole() {
      return role;
   }

   public void setRole(UserRole role) {
      this.role = role;
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
      return "MemberChangeFieldDTO [isStatusChange=" + isStatusChange + ", memberId=" + memberId + ", paused=" + paused
            + ", role=" + role + ", status=" + status + "]";
   }

}
