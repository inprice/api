package io.inprice.scrapper.api.info;

import java.util.Map;

import io.inprice.scrapper.api.helpers.SqlHelper;

public class SearchModel {

   private String term;
   private int lastRowNo;
   private String orderBy;
   private String orderDir;

   public final int ROW_LIMIT = 3;

   public SearchModel(Map<String, String> map, Class<?> clazz) {
      term = SqlHelper.clear(map.getOrDefault("term", "").trim());
      lastRowNo = Integer.parseInt(map.getOrDefault("pageNo", "1"));
      orderBy = SqlHelper.clear(map.getOrDefault("orderBy", "id").trim());
      orderDir = SqlHelper.clear(map.getOrDefault("orderDir", "asc").trim());

      // clearance
      if (term.length() > 500) term = term.substring(0, 500);
      if (lastRowNo < 1) lastRowNo = 1;
      if (!orderDir.matches("(a|de)sc")) orderDir = "asc";
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

   @Override
   public String toString() {
      return "SearchModel [orderBy=" + orderBy + ", orderDir=" + orderDir + ", lastRowNo=" + lastRowNo + ", term=" + term + "]";
   }

}
