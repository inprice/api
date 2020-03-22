package io.inprice.scrapper.api.app.product_import;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class ImportProduct implements Serializable {

   private static final long serialVersionUID = 1099808351397302194L;

   private Long id;
   private ImportType importType;
   private int status;
   private String result;
   private int totalCount = 0;
   private int insertCount = 0;
   private int duplicateCount = 0;
   private int problemCount = 0;
   private Long companyId;
   private Date createdAt;

   private List<String> problemList;
   private List<ImportProductRow> rowList;

   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public ImportType getImportType() {
      return importType;
   }

   public void setImportType(ImportType importType) {
      this.importType = importType;
   }

   public int getStatus() {
      return status;
   }

   public void setStatus(int status) {
      this.status = status;
   }

   public String getResult() {
      return result;
   }

   public void setResult(String result) {
      this.result = result;
   }

   public int getTotalCount() {
      return totalCount;
   }

   public void setTotalCount(int totalCount) {
      this.totalCount = totalCount;
   }

   public int getInsertCount() {
      return insertCount;
   }

   public void setInsertCount(int insertCount) {
      this.insertCount = insertCount;
   }

   public int getDuplicateCount() {
      return duplicateCount;
   }

   public void setDuplicateCount(int duplicateCount) {
      this.duplicateCount = duplicateCount;
   }

   public int getProblemCount() {
      return problemCount;
   }

   public void setProblemCount(int problemCount) {
      this.problemCount = problemCount;
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

   public List<String> getProblemList() {
      return problemList;
   }

   public void setProblemList(List<String> problemList) {
      this.problemList = problemList;
   }

   public List<ImportProductRow> getRowList() {
      return rowList;
   }

   public void setRowList(List<ImportProductRow> rowList) {
      this.rowList = rowList;
   }

}
