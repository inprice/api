package io.inprice.api.scheduled;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Global;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.AccessLog;

/**
 * Persists access logs to db
 * 
 * @since 2021-05-19
 * @author mdpinar
 */
public class AccessLogger implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(AccessLogger.class);

	private static Queue<AccessLog> queue = new LinkedList<>();
  private final String clazz = getClass().getSimpleName();

  @Override
  public void run() {
    if (Global.isTaskRunning(clazz)) {
      logger.warn(clazz + " is already triggered!");
      return;
    }

    synchronized (queue) {
	    try {
	      Global.startTask(clazz);
	
	      logger.info(clazz + " is triggered.");
	      try (Handle handle = Database.getHandle()) {
	      	handle.begin();
	      	
	        PreparedBatch batch = 
	      		handle.prepareBatch(
	    				"insert into access_log (user_id, user_email, user_role, account_id, account_name, ip, agent, path, path_ext, method, req_body, res_body, status, elapsed, slow, created_at) " +
	    				"values (:log.userId, :log.userEmail, :log.userRole, :log.accountId, :log.accountName, :logger.ip, :log.agent, :log.path, :log.pathExt, :log.method, :log.reqBody, :log.resBody, " +
	    				":log.status, :logger.elapsed, :log.slow, :log.createdAt)"
						);
	        int remaining = 0;
	        while (queue.isEmpty() != false) {
	        	AccessLog log = queue.poll();
	        	if (log != null) {
	            batch.bindBean("log", log).add();
	            remaining++;
	            if (remaining >= 100) {
	            	batch.execute();
	            	remaining = 0;
	            	Thread.sleep(500);
	            }
	        	}
	        }
	        if (batch.size() > 0) batch.execute();

	        handle.commit();
	      } catch (Exception e) {
	        logger.error("Failed to trigger " + clazz , e);
	      }
	      
	    } finally {
	      Global.stopTask(clazz);
	    }
		}
  }

  public static void add(AccessLog accessLog) {
    synchronized (queue) {
	  	accessLog.setCreatedAt(new Date());
	  	queue.add(accessLog);
    }
  }

}
