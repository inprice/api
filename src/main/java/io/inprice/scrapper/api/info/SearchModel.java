package io.inprice.scrapper.api.info;

import java.util.Map;

import io.inprice.scrapper.api.utils.SqlHelper;

public class SearchModel {

   private String term;
   private int rowCount;
   private int pageNo;
   private String orderBy;
   private String orderDir;

   public SearchModel(Map<String, String> map, Class<?> clazz) {
      term = SqlHelper.clear(map.getOrDefault("term", "").trim());
      rowCount = Integer.parseInt(map.getOrDefault("rowCount", "10"));
      pageNo = Integer.parseInt(map.getOrDefault("pageNo", "1"));
      orderBy = SqlHelper.clear(map.getOrDefault("orderBy", "id").trim());
      orderDir = SqlHelper.clear(map.getOrDefault("orderDir", "asc").trim());

      // clearance
      if (term.length() > 500)
         term = term.substring(0, 500);
      if (rowCount < 10 || rowCount > 100)
         rowCount = 10;
      if (pageNo < 1)
         rowCount = 1;
      if (!orderDir.matches("(a|de)sc"))
         orderDir = "asc";

      try {
         clazz.getField(orderBy);
      } catch (NoSuchFieldException e) {
         orderBy = "id";
      }
   }

   public String getTerm() {
      return term;
   }

   public int getRowCount() {
      return rowCount;
   }

   public int getPageNo() {
      return pageNo;
   }

   public String getOrderBy() {
      return orderBy;
   }

   public String getOrderDir() {
      return orderDir;
   }

}
