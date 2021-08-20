package io.inprice.api.scheduled;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.config.Props;
import io.inprice.common.utils.DateUtils;

public class TaskManager {

  private static final Logger logger = LoggerFactory.getLogger(TaskManager.class);

  private static ScheduledExecutorService scheduler;

  public static void start() {
    logger.info("TaskManager is starting...");

    scheduler = Executors.newScheduledThreadPool(1);

    TaskDef accessLoggerTask = TaskDef.builder()
			.task(new AccessLogger())
			.delay(1)
			.timePeriod(DateUtils.parseTimePeriod(Props.getConfig().INTERVALS.FLUSHING_ACCESS_LOGS))
		.build();

    scheduler.scheduleAtFixedRate(
  		accessLoggerTask.getTask(), 
  		accessLoggerTask.getDelay(), 
  		accessLoggerTask.getTimePeriod().getInterval(), 
  		accessLoggerTask.getTimePeriod().getTimeUnit()
		);

    logger.info("TaskManager is started with access logger workers.");
  }

  public static void stop() {
    try {
      scheduler.shutdown();
    } catch (SecurityException e) {
      logger.error("Failed to stop TaskManager's scheduler.", e);
    }
  }

}
