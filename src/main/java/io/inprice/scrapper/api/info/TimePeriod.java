package io.inprice.scrapper.api.info;

import java.util.concurrent.TimeUnit;

public class TimePeriod {

   private int interval;
   private TimeUnit timeUnit;

   public TimePeriod(int interval, TimeUnit timeUnit) {
      this.interval = interval;
      this.timeUnit = timeUnit;
   }

   public int getInterval() {
      return interval;
   }

   public TimeUnit getTimeUnit() {
      return timeUnit;
   }
}
