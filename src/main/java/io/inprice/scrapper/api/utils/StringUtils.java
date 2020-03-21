package io.inprice.scrapper.api.utils;

public class StringUtils {

   public static String fixQuotes(String raw) {
      return raw.replaceAll("((?<=(\\{|\\[|\\,|:))\\s*')|('\\s*(?=(\\}|(\\])|(\\,|:))))", "\"");
   }

}
