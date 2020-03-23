package io.inprice.scrapper.api.helpers;

import java.sql.ResultSet;

/**
 * RepositoryHelper
 */
public class RepositoryHelper {

   /**
    * JDBC return 0 when long field is null!!!
    * with this method we can return null it.
   */
   public static Long nullLongHandler(ResultSet rs, String field) {
      try {
         Long val = rs.getLong(field);
         if (! rs.wasNull()) return val;
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
   }

}