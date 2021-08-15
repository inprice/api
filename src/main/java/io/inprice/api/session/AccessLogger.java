package io.inprice.api.session;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import io.inprice.common.models.AccessLog;

/**
 * TODO: db writing code must be added! 
 * 
 * @since 2021-08-15
 * @author mdpinar
 *
 */
public class AccessLogger {

	private static Queue<AccessLog> queue = new LinkedBlockingQueue<>();

  public static void add(AccessLog accessLog) {
  	accessLog.setCreatedAt(new Date());
  	queue.add(accessLog);
  }
	
}
