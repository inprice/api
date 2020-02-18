package io.inprice.scrapper.api.utils;

import java.util.HashMap;
import java.util.Map;

import io.inprice.scrapper.api.info.SearchModel;

public class SqlHelper {

   public static final Map<String, String> STANDARD_SEARCH_MAP;

   static {
      STANDARD_SEARCH_MAP = new HashMap<>();
      STANDARD_SEARCH_MAP.put("term", "");
      STANDARD_SEARCH_MAP.put("page", "1");
      STANDARD_SEARCH_MAP.put("rowCount", "10");
      STANDARD_SEARCH_MAP.put("orderBy", "id");
      STANDARD_SEARCH_MAP.put("orderDir", "asc");
   }

   // http://www.java2s.com/Code/Java/Database-SQL-JDBC/EscapeSQL.htm
   public static String clear(String val) {
      int length = val.length();
      int newLength = length;
      // first check for characters that might
      // be dangerous and calculate a length
      // of the string that has escapes.
      for (int i = 0; i < length; i++) {
         char c = val.charAt(i);
         switch (c) {
         case '\\':
         case '\"':
         case '\'':
         case '\0': {
            newLength += 1;
         }
            break;
         }
      }
      if (length == newLength) {
         // nothing to escape in the string
         return val;
      }
      StringBuffer sb = new StringBuffer(newLength);
      for (int i = 0; i < length; i++) {
         char c = val.charAt(i);
         switch (c) {
         case '\\': {
            sb.append("\\\\");
         }
            break;
         case '\"': {
            sb.append("\\\"");
         }
            break;
         case '\'': {
            sb.append("\\\'");
         }
            break;
         case '\0': {
            sb.append("\\0");
         }
            break;
         default: {
            sb.append(c);
         }
         }
      }
      return sb.toString();
   }

   public static String generateSearchQuery(SearchModel searchModel, String... queryingFields) {
      StringBuilder sql = new StringBuilder("");

      // query string part
      if (!searchModel.getTerm().isEmpty()) {
         sql.append(" and (");
         for (int i = 0; i < queryingFields.length; i++) {
            String field = queryingFields[i];
            sql.append(field);
            sql.append(" like '%");
            sql.append(searchModel.getTerm());
            sql.append("%' ");
            if (i < queryingFields.length - 1) {
               sql.append(" OR ");
            }
         }
         sql.append(") ");
      }

      // ordering part
      if (!searchModel.getOrderBy().isEmpty()) {
         sql.append(" order by ");
         sql.append(searchModel.getOrderBy());
         sql.append(" ");
         sql.append(searchModel.getOrderDir());
      }

      // limiting part
      int page = (searchModel.getPageNo() - 1) * searchModel.getRowCount();
      sql.append(" limit ");
      if (page > 0) {
         sql.append(page);
         sql.append(", ");
      }
      sql.append(page + searchModel.getRowCount());

      return sql.toString();
   }

}
