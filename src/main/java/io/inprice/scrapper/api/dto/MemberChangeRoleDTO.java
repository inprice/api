package io.inprice.scrapper.api.dto;

import java.io.Serializable;

import io.inprice.scrapper.api.app.user.UserRole;

/**
 * MemberChangeRoleDTO
 */
public class MemberChangeRoleDTO implements Serializable {

   private static final long serialVersionUID = -7922528699777216078L;

   private Long memberId;
   private UserRole role;

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
   
}