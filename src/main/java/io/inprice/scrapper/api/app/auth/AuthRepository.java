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
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.ServiceResponse;

public class AuthRepository {

   private static final Logger log = LoggerFactory.getLogger(AuthRepository.class);

   private final Database db = Beans.getSingleton(Database.class);

   public ServiceResponse findByToken(String token) {
      UserSession ses = RedisClient.getSession(token);
      if (ses != null) {
         long diffInMillies = Math.abs(System.currentTimeMillis() - ses.getAccessedAt().getTime());
         long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
         if (diff > 7 && RedisClient.refreshSesion(ses.getToken())) {
            return refreshAccessedAt(ses.getToken());
         } else {
            return Responses.OK;
         }
      }
      return Responses.NotFound.DATA;
   }

   public boolean deleteSession(AuthUser authUser) {
      List<String> deletedList = new ArrayList<>(authUser.getCompanies().size());

      for (UserCompany ms : authUser.getCompanies().values()) {
         RedisClient.removeSesion(ms.getToken());
         deletedList.add(ms.getToken());
      }

      if (deletedList.size() > 0) {
         String inClause = StringUtils.join(deletedList, "', '");
         return
            db.executeQuery(
               String.format("delete from user_session where token in ('%s')", inClause),
                  String.format("Failed to delete user session info by token ('%s')", inClause)
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
               RedisClient.removeSesion(ses.getToken());
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
               RedisClient.removeSesion(ses.getToken());
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
               "insert into user_session (token, user_id, company_id, ip, os, browser) values ('%s', %d, %d, '%s', '%s', '%s')",
               uses.getToken(), uses.getUserId(), uses.getCompanyId(), uses.getIp(), uses.getOs(), uses.getBrowser()
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

   private ServiceResponse refreshAccessedAt(String token) {
      boolean result =
         db.executeQuery(
            "update user_session set accessed_at = now() where token = '" + token + "'", 
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