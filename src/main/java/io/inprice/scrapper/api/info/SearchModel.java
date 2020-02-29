package io.inprice.scrapper.api.info;

import java.util.Map;

import io.inprice.scrapper.api.utils.SqlHelper;

public class SearchModel {

   private String term;
   private int rowLimit;
   private int pageNo;
   private String orderBy;
   private String orderDir;

   public SearchModel(Map<String, String> map, Class<?> clazz) {
      term = SqlHelper.clear(map.getOrDefault("term", "").trim());
      rowLimit = Integer.parseInt(map.getOrDefault("rowLimit", "10"));
      pageNo = Integer.parseInt(map.getOrDefault("pageNo", "1"));
      orderBy = SqlHelper.clear(map.getOrDefault("orderBy", "id").trim());
      orderDir = SqlHelper.clear(map.getOrDefault("orderDir", "asc").trim());

      // clearance
      if (term.length() > 500)
         term = term.substring(0, 500);
      if (rowLimit < 10 || rowLimit > 250)
         rowLimit = 10;
      if (pageNo < 1)
         rowLimit = 1;
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

   public int getRowLimit() {
      return rowLimit;
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

   @Override
   public String toString() {
      return "SearchModel [orderBy=" + orderBy + ", orderDir=" + orderDir + ", pageNo=" + pageNo + ", rowLimit="
            + rowLimit + ", term=" + term + "]";
   }

}
