package io.inprice.scrapper.api.consts;

import java.io.IOException;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Global {

   public static volatile boolean isApplicationRunning = false;

   private static ObjectMapper om;
   private static Object lock = new Object();

   public static ObjectMapper getObjectMapper() {
      if (om == null) {
         synchronized (lock) {
            if (om == null) {
               om = new ObjectMapper()
                  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                  .setSerializationInclusion(Include.NON_NULL)
                  .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            }
         }
      }
      return om;
   }

   public static <T> T fromJson(String value, Class<T> clazz) {
      try {
         return getObjectMapper().readValue(value, clazz);
      } catch (IOException ignored) { }
      return null;
   }

   public static <T> String toJson(T value) {
      try {
         return getObjectMapper().writeValueAsString(value);
      } catch (JsonProcessingException ignored) { }
      return null;
   }

}
