package io.inprice.api.session;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import io.inprice.common.models.AccessLog;

/**
 * TODO: db writing code must be added! 
 * 
 * @since 2021-08-15
 * @author mdpinar
 *
 */
public class AccessLogger {

	private static Queue<AccessLog> queue = new LinkedList<>();

  public static void add(AccessLog accessLog) {
  	accessLog.setCreatedAt(new Date());
  	queue.add(accessLog);
  }
	
}
