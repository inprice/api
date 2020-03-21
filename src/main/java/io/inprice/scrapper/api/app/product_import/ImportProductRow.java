package io.inprice.scrapper.api.app.product_import;

import java.io.Serializable;
import java.util.Date;

import io.inprice.scrapper.api.app.link.LinkStatus;

public class ImportProductRow implements Serializable {

   private static final long serialVersionUID = -7516082962212789090L;

   private Long id;
   private Long importId;
   private ImportType importType;
   private String data;
   private LinkStatus status = LinkStatus.NEW;
   private Date lastUpdate;
   private String description;
   private Long linkId;
   private Long companyId;

   private Object productDTO;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public Long getImportId() {
      return importId;
   }

   public void setImportId(Long importId) {
      this.importId = importId;
   }

   public ImportType getImportType() {
      return importType;
   }

   public void setImportType(ImportType importType) {
      this.importType = importType;
   }

   public String getData() {
      return data;
   }

   public void setData(String data) {
      this.data = data;
   }

   public LinkStatus getStatus() {
      return status;
   }

   public void setStatus(LinkStatus status) {
      this.status = status;
   }

   public Date getLastUpdate() {
      return lastUpdate;
   }

   public void setLastUpdate(Date lastUpdate) {
      this.lastUpdate = lastUpdate;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public Long getLinkId() {
      return linkId;
   }

   public void setLinkId(Long linkId) {
      this.linkId = linkId;
   }

   public Long getCompanyId() {
      return companyId;
   }

   public void setCompanyId(Long companyId) {
      this.companyId = companyId;
   }

   public Object getProductDTO() {
      return productDTO;
   }

   public void setProductDTO(Object productDTO) {
      this.productDTO = productDTO;
   }

}
