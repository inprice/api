package io.inprice.scrapper.api.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.meta.TimePeriod;

/**
 * Utility class to provide functionality for date formatting operations
 *
 * @author mdpinar
 */
public class DateUtils {

   private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

   private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
   private static final SimpleDateFormat sdfForDB = new SimpleDateFormat("yyyy-MM-dd");
   private static final SimpleDateFormat sdfLongForDB = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
   private static final SimpleDateFormat sdfLongForLogging = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
   private static final SimpleDateFormat sdfStandart = new SimpleDateFormat("dd/MM/yyyy");

   public static String formatDateForDB(Date date) {
      if (date != null)
         return "'" + sdfForDB.format(date) + "'";
      else
         return "";
   }

   public static String formatLongDate(Date date) {
      if (date != null)
         return sdfLongForDB.format(date);
      else
         return "";
   }

   public static String formatLongDateForDB(Date date) {
      if (date != null)
         return "'" + sdfLongForDB.format(date) + "'";
      else
         return "";
   }

   public static String formatDateStandart(Date date) {
      if (date != null)
         return sdfStandart.format(date);
      else
         return "";
   }

   public static String formatDate(Date date, String format) {
      if (date != null) {
         SimpleDateFormat formatPattern = new SimpleDateFormat(format);
         return formatPattern.format(date);
      } else {
         return "";
      }
   }

   public static String formatReverseDate(Date date) {
      if (date != null)
         return sdfForDB.format(date);
      else
         return "";
   }

   public static String nowForLogging() {
      return sdfLongForLogging.format(new Date());
   }

   public static String today(String format) {
      return new SimpleDateFormat(format).format(new Date());
   }

   public static Date today() {
      return new Date();
   }

   public static Date getFirstDayOfMonth() {
      Calendar now = Calendar.getInstance();
      now.set(Calendar.DAY_OF_MONTH, 1);

      return now.getTime();
   }

   public static Date getFirstDayOfYear() {
      Calendar now = Calendar.getInstance();
      now.set(Calendar.DAY_OF_MONTH, 1);
      now.set(Calendar.MONTH, 0);

      return now.getTime();
   }

   public static Date findFirstDay(String yyyymm) {
      try {
         return sdf.parse(yyyymm + "/01");
      } catch (ParseException e) {
         ;
      }

      return null;
   }

   public static Date findLastDay(String yyyymm) {
      for (int i = 31; i > 27; i--) {
         try {
            return sdf.parse(yyyymm + "/" + i);
         } catch (ParseException e) {
            ;
         }
      }

      return null;
   }

   public static Date parse(String date, String format) {
      try {
         return new SimpleDateFormat(format).parse(date);
      } catch (ParseException e) {
         logger.error(e.getMessage());
      }

      return null;
   }

   /**
    * Converts shortened time statement to its long equivalent as SECONDS
    *
    * 1S or 1s means one second 1M or 1m means one minute 1H or 1h means one hour
    * 1D or 1d means one day
    */
   public static TimePeriod parseTimePeriod(String timePeriodStatement) {
      final char type = timePeriodStatement.charAt(timePeriodStatement.length() - 1);

      TimeUnit timeUnit = TimeUnit.SECONDS;
      switch (type) {
         case 'm':
         case 'M': {
            timeUnit = TimeUnit.MINUTES;
            break;
         }
         case 'h':
         case 'H': {
            timeUnit = TimeUnit.HOURS;
            break;
         }
         case 'd':
         case 'D': {
            timeUnit = TimeUnit.DAYS;
            break;
         }
      }
      int interval = NumberUtils.toInteger(timePeriodStatement.substring(0, timePeriodStatement.length() - 1));
      return new TimePeriod(interval, timeUnit);
   }

   public static Long parseTimePeriodAsMillis(String timePeriodStatement) {
      TimePeriod tp = parseTimePeriod(timePeriodStatement);

      // SECONDS
      long val = tp.getInterval() * 1000;

      if (tp.getTimeUnit().ordinal() >= TimeUnit.MINUTES.ordinal())
         val *= 60;
      if (tp.getTimeUnit().ordinal() >= TimeUnit.HOURS.ordinal())
         val *= 60;
      if (tp.getTimeUnit().ordinal() == TimeUnit.DAYS.ordinal())
         val *= 24;

      return val;
   }

}
