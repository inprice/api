package io.inprice.api.helpers;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.inprice.api.session.CurrentUser;
import io.inprice.api.info.SearchModel;

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
     if (StringUtils.isBlank(val)) return val;

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
      return sb.toString().trim();
   }

   public static String generateSearchQuery(SearchModel searchModel) {
    return generateSearchQuery(searchModel, null);
   }

   public static String generateSearchQuery(SearchModel searchModel, String extraConditions) {
      StringBuilder sql = new StringBuilder();
      if (StringUtils.isBlank(searchModel.getQuery())) {
        sql.append("select * from ");
        sql.append(searchModel.getTable());
      } else {
        sql.append(searchModel.getQuery());
      }
      sql.append(" where ");
      if (StringUtils.isNotBlank(searchModel.getPrefixForCompanyId())) {
        sql.append(searchModel.getPrefixForCompanyId());
        sql.append(".");
      }
      sql.append("company_id = ");
      sql.append(CurrentUser.getCompanyId());

      if (StringUtils.isNotBlank(extraConditions)) {
        sql.append(" and ");
        sql.append(extraConditions);
      }

      // query string part
      if (StringUtils.isNotBlank(searchModel.getTerm()) && !searchModel.getTerm().equals("null")) {
         sql.append(" and (");
         for (int i=0; i<searchModel.getFields().size(); i++) {
            String field = searchModel.getFields().get(i);
            sql.append(field);
            if (searchModel.isExactSearch()) {
              sql.append(" = '");
              sql.append(searchModel.getTerm());
              sql.append("' ");
            } else {
              sql.append(" like '%");
              sql.append(searchModel.getTerm());
              sql.append("%' ");
            }
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
