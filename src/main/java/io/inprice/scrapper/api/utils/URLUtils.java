package io.inprice.scrapper.api.utils;

public class URLUtils {

   public static boolean isAValidURL(String url) {
      if (url == null || url.trim().isEmpty() || url.trim().length() < 20 || url.trim().length() > 1024)
         return false;
      return url.matches("^(http|https)://.*$");
   }

   public static String extractDomain(String url) {
      final String newForm = url.replaceAll("^(https?)://|www.", "");
      final String[] chunks = newForm.split("/");

      if (chunks.length > 0)
         return chunks[0];

      return null;
   }

}
