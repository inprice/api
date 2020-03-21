package io.inprice.scrapper.api.app.link;

import java.io.Serializable;
import java.util.Date;

public class LinkHistory implements Serializable {

   private static final long serialVersionUID = 885057701505049080L;

   private Long id;
   private Long linkId;
   private LinkStatus status = LinkStatus.NEW;
   private Integer httpStatus;
   private Long productId;
   private Long companyId;
   private Date createdAt;

   public LinkHistory() {
   }

   public LinkHistory(LinkStatus status) {
      this.status = status;
   }

   public LinkHistory(Long linkId, LinkStatus status) {
      this.linkId = linkId;
      this.status = status;
   }

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getLinkId() {
      return linkId;
   }

   public void setLinkId(Long linkId) {
      this.linkId = linkId;
   }

   public LinkStatus getStatus() {
      return status;
   }

   public void setStatus(LinkStatus status) {
      this.status = status;
   }

   public Integer getHttpStatus() {
      return httpStatus;
   }

   public void setHttpStatus(Integer httpStatus) {
      this.httpStatus = httpStatus;
   }

   public Long getProductId() {
      return productId;
   }

   public void setProductId(Long productId) {
      this.productId = productId;
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

   public Date getCreatedAt() {
      return createdAt;
   }

   public void setCreatedAt(Date createdAt) {
      this.createdAt = createdAt;
   }

}
