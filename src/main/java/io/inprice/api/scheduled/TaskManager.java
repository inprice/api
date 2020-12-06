package io.inprice.api.scheduled;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.external.Props;
import io.inprice.common.info.TimePeriod;
import io.inprice.common.utils.DateUtils;

public class TaskManager {

  private static final Logger log = LoggerFactory.getLogger(TaskManager.class);

  private static ScheduledExecutorService scheduler;

  public static void start() {
    log.info("TaskManager is starting...");
    int corePoolSize = 1;
    scheduler = Executors.newScheduledThreadPool(corePoolSize);
    loadTask(new ReminderForFreeCompanies(), 0, DateUtils.parseTimePeriod(Props.TIME_PERIOD_OF_REMINDER_FOR_FREE_COMPANIES()));
    log.info("TaskManager is started with {} workers.", corePoolSize);
  }

  private static void loadTask(Runnable task, int delay, TimePeriod timePeriod) {
    scheduler.scheduleAtFixedRate(task, delay, timePeriod.getInterval(), timePeriod.getTimeUnit());
  }

  public static void stop() {
    try {
      scheduler.shutdown();
    } catch (SecurityException e) {
      log.error("Failed to stop TaskManager's scheduler.", e);
    }
  }

}
