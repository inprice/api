package io.inprice.scrapper.api.helpers;

import java.util.HashMap;
import java.util.Map;

import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.info.SearchModel;

public class SqlHelper {

   public static final Map<String, String> STANDARD_SEARCH_MAP;

   static {
      STANDARD_SEARCH_MAP = new HashMap<>();
      STANDARD_SEARCH_MAP.put("term", "");
      STANDARD_SEARCH_MAP.put("lastRowNo", "0");
      STANDARD_SEARCH_MAP.put("orderBy", "id");
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

   public static String generateSearchQuery(SearchModel searchModel) {
      StringBuilder sql = new StringBuilder("select * from ");
      sql.append(searchModel.getTable());
      sql.append(" where company_id = ");
      sql.append(CurrentUser.getCompanyId());

      // query string part
      if (!searchModel.getTerm().isEmpty()) {
         sql.append(" and (");
         for (int i=0; i<searchModel.getFields().size(); i++) {
            String field = searchModel.getFields().get(i);
            sql.append(field);
            sql.append(" like '%");
            sql.append(searchModel.getTerm());
            sql.append("%' ");
            if (i < searchModel.getFields().size() - 1) {
               sql.append(" OR ");
            }
         }
         sql.append(") ");
      }

      // ordering part
      if (!searchModel.getOrderBy().isEmpty()) {
         sql.append(" order by ");
         sql.append(searchModel.getOrderBy());
      }

      // limiting part
      sql.append(" limit ");
      sql.append(searchModel.getLastRowNo());
      sql.append(", ");
      sql.append(searchModel.ROW_LIMIT);

      return sql.toString();
   }

}