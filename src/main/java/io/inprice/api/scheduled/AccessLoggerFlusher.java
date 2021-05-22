package io.inprice.api.scheduled;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Global;
import io.inprice.api.external.RedisClient;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.analytics.AccessLog;

/**
 * Persists access logs to db
 * 
 * @since 2021-05-19
 * @author mdpinar
 */
public class AccessLoggerFlusher implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(AccessLoggerFlusher.class);

  private final String clazz = getClass().getSimpleName();

  @Override
  public void run() {
    if (Global.isTaskRunning(clazz)) {
      log.warn(clazz + " is already triggered!");
      return;
    }

    try {
      Global.startTask(clazz);

      log.info(clazz + " is triggered.");
      try (Handle handle = Database.getHandle()) {
      	handle.begin();
      	
        PreparedBatch batch = 
      		handle.prepareBatch(
    				"insert into analytics_access_log (user_id, user_email, user_role, account_id, account_name, ip, path, path_ext, method, req_body, res_body, status, elapsed, created_at) "+
    				"values (:log.userId, :log.userEmail, :log.userRole, :log.accountId, :log.accountName, :log.ip, :log.path, :log.pathExt, :log.method, :log.reqBody, :log.resBody, :log.status, :log.elapsed, :log.createdAt)"
					);
        int remaining = 0;
        while (! RedisClient.userLogQueue.isEmpty()) {
        	AccessLog log = RedisClient.userLogQueue.poll();
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
        log.error("Failed to trigger " + clazz , e);
      }
      
    } finally {
      Global.stopTask(clazz);
    }
  }

}
