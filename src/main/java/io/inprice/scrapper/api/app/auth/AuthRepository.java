package io.inprice.scrapper.api.app.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;

public class AuthRepository {

   private static final Logger log = LoggerFactory.getLogger(AuthRepository.class);

   private final Database db = Beans.getSingleton(Database.class);

   public ServiceResponse findByToken(String token) {
      String md5Hash = DigestUtils.md5Hex(token);
      UserSession ses = RedisClient.getSession(md5Hash);
      if (ses != null) {
         long diffInMillies = Math.abs(System.currentTimeMillis() - ses.getAccessedAt().getTime());
         long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
         if (diff > 7 && RedisClient.refreshSesion(ses.getTokenHash())) {
            return refreshAccessedAt(ses.getTokenHash());
         } else {
            return Responses.OK;
         }
      }
      return Responses.NotFound.DATA;
   }

   public boolean deleteByTokenHashes(List<String> tokens) {
      List<String> hashList = new ArrayList<>(tokens.size());
      for (String token : tokens) {
         if (StringUtils.isNotBlank(token)) {
            String md5Hash = DigestUtils.md5Hex(token);
            if (RedisClient.removeSesion(md5Hash)) {
               hashList.add(md5Hash);
            }
         }
      }
      if (hashList.size() > 0) {
         String inClause = StringUtils.join(hashList, "', '");
         return
            db.executeQuery(
               String.format("delete from user_session where token_hash in ('%s')", inClause),
                  String.format("Failed to delete user session info by token hashes ('%s')", inClause)
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
               RedisClient.removeSesion(ses.getTokenHash());
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
               RedisClient.removeSesion(ses.getTokenHash());
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

   public ServiceResponse saveSession(UserSession session) {
      boolean isAdded = RedisClient.addSesion(session);

      if (isAdded) {
         String query = "insert into user_session "
               + "(token_hash, user_id, company_id, ip, os, browser) values (?, ?, ?, ?, ?, ?)";
         try (Connection con = db.getConnection();
            PreparedStatement pst = con.prepareStatement(query)) {
            int i = 0;
            pst.setString(++i, session.getTokenHash());
            pst.setLong(++i, session.getUserId());
            pst.setLong(++i, session.getCompanyId());
            pst.setString(++i, session.getIp());
            pst.setString(++i, session.getOs());
            pst.setString(++i, session.getBrowser());

            if (pst.executeUpdate() > 0) {
               return Responses.OK;
            }
         } catch (Exception e) {
            log.error(session.toString());
            log.error("Failed to insert a session", e);
         }
      } else {
         return Responses.DataProblem.REDIS_PROBLEM;
      }

      return Responses.DataProblem.DB_PROBLEM;
   }

   private ServiceResponse refreshAccessedAt(String md5Hash) {
      boolean result =
         db.executeQuery(
            "update user_session set accessed_at = now() where token_hash = '" + md5Hash + "'", 
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
         model.setTokenHash(rs.getString("token_hash"));
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