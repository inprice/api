package io.inprice.scrapper.api.dto;

import io.inprice.scrapper.api.app.member.MemberStatus;
import io.inprice.scrapper.api.app.member.MemberRole;

public class MemberChangeFieldDTO {

   private boolean isStatusChange;
   private Long memberId;
   private MemberRole role;
   private MemberStatus status;
   private boolean undo;

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

   public MemberRole getRole() {
      return role;
   }

   public void setRole(MemberRole role) {
      this.role = role;
   }

   public MemberStatus getStatus() {
      return status;
   }

   public void setStatus(MemberStatus status) {
      this.status = status;
   }

   public boolean isUndo() {
      return undo;
   }

   public void setUndo(boolean undo) {
      this.undo = undo;
   }

   @Override
   public String toString() {
      return "MemberChangeFieldDTO [isStatusChange=" + isStatusChange + ", memberId=" + memberId + ", role=" + role
            + ", status=" + status + ", undo=" + undo + "]";
   }

}
