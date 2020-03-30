package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import io.inprice.scrapper.api.app.member.MemberRole;

/**
 * MemberChangeRoleDTO
 */
public class MemberChangeRoleDTO implements Serializable {

   private static final long serialVersionUID = -7922528699777216078L;

   private Long memberId;
   private MemberRole role;

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
   
}