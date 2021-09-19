package io.inprice.api.helpers;

import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.config.Props;
import io.inprice.common.helpers.Database;
import io.inprice.common.models.AccessLog;

/**
 * Persists access logs to db
 * 
 * @since 2021-05-19
 * @author mdpinar
 */
public class AccessLogger {

  private static final Logger logger = LoggerFactory.getLogger(AccessLogger.class);

	private static Queue<AccessLog> queue = new LinkedList<>();
  private static Object lock = new Object();

  public static void add(AccessLog accessLog) {
  	if (lock == null) return;

  	synchronized (lock) {
	  	accessLog.setCreatedAt(new Date());
	  	queue.add(accessLog);
	  	if (queue.size() >= Props.getConfig().THRESHOLDS.ACCESS_LOG_ROW_LIMIT) flush(false);
  	}
	}

  public static void flush(boolean isTerminating) {
  	if (isTerminating) lock = null;

		if (queue.size() > 0) {
      try (Handle handle = Database.getHandle()) {
        PreparedBatch batch = 
      		handle.prepareBatch(
    				"insert into access_log (user_id, user_email, user_role, workspace_id, workspace_name, ip, agent, path, path_ext, method, req_body, res_body, status, elapsed, slow, created_at) " +
    				"values (:log.userId, :log.userEmail, :log.userRole, :log.workspaceId, :log.workspaceName, :logger.ip, :log.agent, :log.path, :log.pathExt, :log.method, :log.reqBody, :log.resBody, " +
    				":log.status, :logger.elapsed, :log.slow, :log.createdAt)"
					);

        while (queue.isEmpty() != false) {
        	AccessLog log = queue.poll();
        	if (log != null) batch.bindBean("log", log).add();
        }

        if (batch.size() > 0) {
        	batch.execute();
        	logger.info("{} access logs persisted to db.", batch.size());
        }

      } catch (Exception e) {
        logger.error("Failed to flush access logs", e);
      }
		}
  }

}
