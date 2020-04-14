package io.inprice.scrapper.api.app.auth;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.user.UserCompany;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;

public class AuthRepository {

   private static final Logger log = LoggerFactory.getLogger(AuthRepository.class);

   private final Database db = Beans.getSingleton(Database.class);

   public ServiceResponse findByHash(String hash) {
      UserSession ses = RedisClient.getSession(hash);
      if (ses != null) {
         long diffInMillies = Math.abs(System.currentTimeMillis() - ses.getAccessedAt().getTime());
         long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
         if (diff > 7 && RedisClient.refreshSesion(ses.getHash())) {
            return refreshAccessedAt(ses.getHash());
         } else {
            return Responses.OK;
         }
      }
      return Responses.NotFound.DATA;
   }

   public boolean deleteSession(AuthUser authUser) {
      List<String> deletedList = new ArrayList<>(authUser.getCompanies().size());

      for (UserCompany uc: authUser.getCompanies()) {
         RedisClient.removeSesion(uc.getHash());
         deletedList.add(uc.getHash());
      }

      if (deletedList.size() > 0) {
         String inClause = StringUtils.join(deletedList, "', '");
         return
            db.executeQuery(
               String.format("delete from user_session where _hash in ('%s')", inClause),
                  String.format("Failed to delete user session info by hash ('%s')", inClause)
            );
      }
      return false;
   }

   public ServiceResponse findByUserId(Long userId) {
      List<UserSession> models = db.findMultiple(String.format("select * from user_session where user_id=%d", userId), AuthRepository::map);
      if (models != null)
         return new ServiceResponse(models);
      else
         return Responses.NotFound.DATA;
   }

   public boolean deleteByUserId(Long userId) {
      try (Connection con = db.getConnection()) {
         List<UserSession> sessions = 
            db.findMultiple(con, String.format("select * from user_session where user_id=%d", userId), AuthRepository::map);
         if (sessions != null && sessions.size() > 0) {
            for (UserSession ses : sessions) {
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

   public boolean deleteByUserAndCompanyId(Long userId, Long companyId) {
      try (Connection con = db.getConnection()) {
         List<UserSession> sessions = 
            db.findMultiple(con, String.format("select * from user_session where user_id=%d and company_id=%d", userId, companyId), AuthRepository::map);
         if (sessions != null && sessions.size() > 0) {
            for (UserSession ses : sessions) {
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

   public ServiceResponse saveSessions(List<UserSession> sessions) {
      boolean isAdded = RedisClient.addSesions(sessions);

      if (isAdded) {
         String[] queries = new String[sessions.size()];
         for (int i = 0; i < sessions.size(); i++) {
            UserSession uses = sessions.get(i);
            queries[i] = String.format(
               "insert into user_session (_hash, user_id, company_id, ip, os, browser) values ('%s', %d, %d, '%s', '%s', '%s')",
               uses.getHash(), uses.getUserId(), uses.getCompanyId(), uses.getIp(), uses.getOs(), uses.getBrowser()
            );
         }
         boolean result = 
            db.executeBatchQueries(
               queries, "Failed to add new sessions for: " + sessions.get(0).getUserId(), sessions.size()
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

   private static UserSession map(ResultSet rs) {
      try {
         UserSession model = new UserSession();
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