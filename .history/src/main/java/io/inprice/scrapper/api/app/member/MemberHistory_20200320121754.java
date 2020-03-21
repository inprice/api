package io.inprice.scrapper.api.app.member;

import java.io.Serializable;
import java.util.Date;

import io.inprice.scrapper.api.app.user.UserRole;

public class MemberHistory implements Serializable {

   private static final long serialVersionUID = -6724246635240270406L;

   private Long id;
   private Long memberId;
   private Long companyId;
   private MemberStatus status;
   private Date createdAt = new Date();

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getMemberId() {
      return memberId;
   }

   public void setMemberId(Long memberId) {
      this.memberId = memberId;
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

   public MemberStatus getStatus() {
      return status;
   }

   public void setStatus(MemberStatus status) {
      this.status = status;
   }

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

}
