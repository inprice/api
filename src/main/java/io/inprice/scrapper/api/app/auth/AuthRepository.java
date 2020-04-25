package io.inprice.scrapper.api.app.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.session.info.ForCookie;
import io.inprice.scrapper.api.session.info.ForDatabase;
import io.inprice.scrapper.api.session.info.ForRedis;
import io.inprice.scrapper.api.utils.DateUtils;

public class AuthRepository {

   private static final Logger log = LoggerFactory.getLogger(AuthRepository.class);

   private final Database db = Beans.getSingleton(Database.class);

   public ServiceResponse findByHash(String hash) {
      ForRedis ses = RedisClient.getSession(hash);
      if (ses != null) {
         long diffInMillies = Math.abs(System.currentTimeMillis() - ses.getAccessedAt().getTime());
         long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
         if (diff > 7 && RedisClient.refreshSesion(ses.getHash())) {
            refreshAccessedAt(ses.getHash());
         }
         return new ServiceResponse(ses);
      }
      return Responses.NotFound.DATA;
   }

   public boolean closeSession(List<ForCookie> sessions) {
      List<String> closed = new ArrayList<>(sessions.size());

      for (ForCookie ses: sessions) {
         RedisClient.removeSesion(ses.getHash());
         closed.add(ses.getHash());
         log.info("Logout {}", ses.toString());
      }

      if (closed.size() > 0) {
         String inClause = StringUtils.join(closed, "', '");
         return
            db.executeQuery(
               String.format("delete from user_session where _hash in ('%s')", inClause),
                  String.format("Failed to delete user session info by hash ('%s')", inClause)
            );
      }
      return false;
   }

   public ServiceResponse findByUserId(Long userId) {
      final String query = "select distinct os, browser, ip, accessed_at from user_session where user_id=? ";
      try (Connection con = db.getConnection();
         PreparedStatement pst = con.prepareStatement(query)) {
         pst.setLong(1, CurrentUser.getUserId());

         try (ResultSet rs = pst.executeQuery()) {
            List<Map<String, String>> data = new ArrayList<>();
            while (rs.next()) {
               Map<String, String> map = new HashMap<>(4);
               map.put("os", rs.getString("os"));
               map.put("browser", rs.getString("browser"));
               map.put("ip", rs.getString("ip"));
               map.put("date", DateUtils.formatLongDate(rs.getTimestamp("accessed_at")));
               data.add(map);
            }
            return new ServiceResponse(data);
         }
      } catch (SQLException e) {
         log.error("Failed to get user session. UserId: " + userId, e);
         return Responses.DataProblem.DB_PROBLEM;
      }
   }

   public boolean closeByUserId(Long userId) {
      try (Connection con = db.getConnection()) {
         List<ForDatabase> sessions = 
            db.findMultiple(con, String.format("select * from user_session where user_id=%d", userId), AuthRepository::map);
         if (sessions != null && sessions.size() > 0) {
            for (ForDatabase ses : sessions) {
               RedisClient.removeSesion(ses.getHash());
            }
            return
               db.executeQuery(
                  con,
                  String.format("delete from user_session where user_id=%d", userId),
                     String.format("Failed to delete a user's session by user id (%d)", userId)
               );
         }
      } catch (Exception e) {
         log.error("Failed to delete a user's session", e);
      }
      return false;
   }

   public boolean closeByUserAndCompanyId(Long userId, Long companyId) {
      try (Connection con = db.getConnection()) {
         List<ForDatabase> sessions = 
            db.findMultiple(con, String.format("select * from user_session where user_id=%d and company_id=%d", userId, companyId), AuthRepository::map);
         if (sessions != null && sessions.size() > 0) {
            for (ForDatabase ses : sessions) {
               RedisClient.removeSesion(ses.getHash());
            }
            return
               db.executeQuery(
                  con,
                  String.format("delete from user_session where user_id=%d and company_id=%d", userId),
                     String.format("Failed to delete a user's session by user id (%d) and company id (%d)", userId, companyId)
               );
         }
      } catch (Exception e) {
         log.error("Failed to delete a user's session", e);
      }
      return false;
   }

   public ServiceResponse saveSessions(List<ForRedis> redisSesList, List<ForDatabase> dbSesList) {
      boolean isAdded = RedisClient.addSesions(redisSesList);

      if (isAdded) {
         String[] queries = new String[dbSesList.size()];
         for (int i = 0; i < dbSesList.size(); i++) {
            ForDatabase uses = dbSesList.get(i);
            queries[i] = String.format(
               "insert into user_session (_hash, user_id, company_id, ip, os, browser, user_agent) values ('%s', %d, %d, '%s', '%s', '%s', '%s')",
               uses.getHash(), uses.getUserId(), uses.getCompanyId(), uses.getIp(), uses.getOs(), uses.getBrowser(), uses.getUserAgent()
            );
         }
         boolean result = 
            db.executeBatchQueries(
               queries, "Failed to add new sessions for: " + dbSesList.get(0), dbSesList.size()
         );

         if (result) return Responses.OK;
      } else {
         return Responses.DataProblem.REDIS_PROBLEM;
      }

      return Responses.DataProblem.DB_PROBLEM;
   }

   private ServiceResponse refreshAccessedAt(String hash) {
      boolean result =
         db.executeQuery(
            "update user_session set accessed_at = now() where _hash = '" + hash + "'", 
            "Failed to refresh a session"
         );

      if (result) {
         return Responses.OK;
      }
      return Responses.DataProblem.DB_PROBLEM;
   }

   private static ForDatabase map(ResultSet rs) {
      try {
         ForDatabase model = new ForDatabase();
         model.setHash(rs.getString("_hash"));
         model.setUserId(rs.getLong("user_id"));
         model.setCompanyId(rs.getLong("company_id"));
         model.setIp(rs.getString("ip"));
         model.setOs(rs.getString("os"));
         model.setBrowser(rs.getString("browser"));
         model.setUserAgent(rs.getString("user_agent"));
         model.setAccessedAt(rs.getTimestamp("accessed_at"));

         return model;
      } catch (SQLException e) {
         log.error("Failed to set user session's properties", e);
      }
      return null;
   }

}