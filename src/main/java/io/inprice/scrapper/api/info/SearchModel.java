package io.inprice.scrapper.api.info;

import java.util.List;
import java.util.Map;

import io.inprice.scrapper.api.helpers.SqlHelper;

public class SearchModel {

   private String table;
   private String term;
   private int lastRowNo;
   private String orderBy;
   private String orderDir;
   private List<String> fields;

   public final int ROW_LIMIT = 3;

   public SearchModel(Map<String, String> map, Class<?> clazz) {
      term = SqlHelper.clear(map.getOrDefault("term", "").trim());
      lastRowNo = Integer.parseInt(map.getOrDefault("lastRowNo", "0"));
      orderBy = SqlHelper.clear(map.getOrDefault("orderBy", "id").trim());
      orderDir = SqlHelper.clear(map.getOrDefault("orderDir", "asc").trim());

      // clearance
      if (term.length() > 255) term = term.substring(0, 255);
      if (lastRowNo < 0 || lastRowNo > 1000) lastRowNo = 0;
      if (!orderDir.matches("(a|de)sc")) orderDir = "asc";
   }

   public String getTable() {
      return table;
   }

   public void setTable(String table) {
      this.table = table;
   }

   public String getTerm() {
      return term;
   }

   public int getLastRowNo() {
      return lastRowNo;
   }

   public String getOrderBy() {
      return orderBy;
   }

   public String getOrderDir() {
      return orderDir;
   }

   public List<String> getFields() {
      return fields;
   }

   public void setFields(List<String> fields) {
      this.fields = fields;
   }

   @Override
   public String toString() {
      return "SearchModel [orderBy=" + orderBy + ", orderDir=" + orderDir + ", table=" + table + ", term=" + term + "]";
   }

}
