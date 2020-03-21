package io.inprice.scrapper.api.utils;

public class NumberUtils {

   public static String extractPrice(String numString) {
      if (numString == null || numString.trim().isEmpty())
         return "0";

      StringBuilder sb = new StringBuilder();
      for (Character ch : numString.toCharArray()) {
         if ((ch >= '0' && ch <= '9') || ch == ',' || ch == '.')
            sb.append(ch);
      }
      String trimmed = sb.toString();
      boolean commaDecimal = (trimmed.length() > 3 && trimmed.charAt(trimmed.length() - 3) == ',');

      String pure = trimmed.replaceAll("[^\\d.]", "");

      if (commaDecimal) {
         int ix = pure.length() - 2;
         return pure.substring(0, ix) + "." + pure.substring(ix);
      } else {
         if (pure.isEmpty())
            return "0";
         else
            return pure;
      }
   }

   public static Integer toInteger(String val) {
      if (val != null && !val.trim().isEmpty()) {
         try {
            return Integer.parseInt(val);
         } catch (Exception e) {
            //
         }
      }
      return null;
   }

   public static Long toLong(String val) {
      if (val != null && !val.trim().isEmpty()) {
         try {
            return Long.parseLong(val);
         } catch (Exception e) {
            //
         }
      }
      return null;
   }

}
