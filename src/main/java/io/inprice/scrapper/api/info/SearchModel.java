package io.inprice.scrapper.api.info;

import java.util.Map;

import io.inprice.scrapper.api.utils.SqlHelper;

public class SearchModel {

   private String term;
   private int pageLimit;
   private int pageNo;
   private String orderBy;
   private String orderDir;

   public SearchModel(Map<String, String> map, Class<?> clazz) {
      term = SqlHelper.clear(map.getOrDefault("term", "").trim());
      pageLimit = Integer.parseInt(map.getOrDefault("pageLimit", "10"));
      pageNo = Integer.parseInt(map.getOrDefault("pageNo", "1"));
      orderBy = SqlHelper.clear(map.getOrDefault("orderBy", "id").trim());
      orderDir = SqlHelper.clear(map.getOrDefault("orderDir", "asc").trim());

      // clearance
      if (term.length() > 500)
         term = term.substring(0, 500);
      if (pageLimit < 10 || pageLimit > 250)
         pageLimit = 10;
      if (pageNo < 1)
         pageLimit = 1;
      if (!orderDir.matches("(a|de)sc"))
         orderDir = "asc";
   }

   public String getTerm() {
      return term;
   }

   public int getPageLimit() {
      return pageLimit;
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
      return "SearchModel [orderBy=" + orderBy + ", orderDir=" + orderDir + ", pageNo=" + pageNo + ", pageLimit="
            + pageLimit + ", term=" + term + "]";
   }

}
