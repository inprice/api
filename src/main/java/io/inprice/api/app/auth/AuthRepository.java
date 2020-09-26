package io.inprice.api.app.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.auth.dto.PasswordDTO;
import io.inprice.api.app.token.TokenService;
import io.inprice.api.app.token.TokenType;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForCookie;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.api.session.info.ForRedis;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;
import jodd.util.BCrypt;

class AuthRepository {

  private static final Logger log = LoggerFactory.getLogger(AuthRepository.class);

  ForRedis findByHash(String hash) {
    ForRedis ses = RedisClient.getSession(hash);
    if (ses != null) {
      long diffInMillies = Math.abs(System.currentTimeMillis() - ses.getAccessedAt().getTime());
      long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
      if (diff > 7 && RedisClient.refreshSesion(ses.getHash())) {
        boolean refreshed = refreshAccessedAt(ses.getHash());
        if (!refreshed) {
          log.warn("Failed to refresh accessed date for {}", hash);
        }
      }
      return ses;
    }
    return null;
  }

  boolean closeSession(List<ForCookie> sessions) {
    List<String> hashList = new ArrayList<>(sessions.size());

    for (ForCookie ses : sessions) {
      RedisClient.removeSesion(ses.getHash());
      hashList.add(ses.getHash());
      log.info("Logout {}", ses.toString());
    }

    boolean updated = false;
    if (hashList.size() > 0) {
      try (Handle handle = Database.getHandle()) {
        AuthDao dao = handle.attach(AuthDao.class);
        updated = dao.deleteSessionByHashList(hashList);
      }
    }
    return updated;
  }

  List<ForDatabase> findOpenedSessions(List<String> excludedHashes) {
    try (Handle handle = Database.getHandle()) {
      AuthDao dao = handle.attach(AuthDao.class);
      List<ForDatabase> openedSessions = dao.getOpenedSessions(CurrentUser.getUserId(), excludedHashes);
      return openedSessions;
    } catch (Exception e) {
      log.error("Failed to get user session.", e);
    }
    return null;
  }

  boolean closeByUserId(Long userId) {
    try (Handle handle = Database.getHandle()) {
      AuthDao dao = handle.attach(AuthDao.class);
      List<ForDatabase> sessions = dao.getUserSessions(userId);
      if (sessions != null && sessions.size() > 0) {
        for (ForDatabase ses : sessions) {
          RedisClient.removeSesion(ses.getHash());
        }
        return dao.deleteSessionByUserId(userId);
      }
    } catch (Exception e) {
      log.error("Failed to delete a user's session", e);
    }
    return false;
  }

  boolean saveSessions(List<ForRedis> redisSesList, List<ForDatabase> dbSesList) {
    boolean isAdded = RedisClient.addSesions(redisSesList);

    if (isAdded) {
      try (Handle handle = Database.getHandle()) {
        AuthDao dao = handle.attach(AuthDao.class);
        boolean[] anyAffected = dao.addSessions(dbSesList);
        if (anyAffected != null && anyAffected.length > 0) {
          for (boolean b : anyAffected) {
            if (b) return true;
          }
        }
      }
    }

    return false;
  }

  User updateUserPassword(PasswordDTO dto) {
    try (Handle handle = Database.getHandle()) {
      AuthDao dao = handle.attach(AuthDao.class);

      final String email = TokenService.get(TokenType.FORGOT_PASSWORD, dto.getToken());
      if (email != null) {
        User user = dao.findUserByEmail(email);
        if (user != null) {
          final String salt = BCrypt.gensalt(Props.APP_SALT_ROUNDS());
          final String hash = BCrypt.hashpw(dto.getPassword(), salt);
          boolean isOK = dao.updateUserPassword(user.getId(), salt, hash);
          if (isOK) return user;
        }
      }
    }
    return null;
  }

  User findUserByEmail(String email) {
    try (Handle handle = Database.getHandle()) {
      AuthDao dao = handle.attach(AuthDao.class);
      return dao.findUserByEmail(email);
    }
  }

  List<Membership> getUserMembershipsByEmail(String email) {
    try (Handle handle = Database.getHandle()) {
      AuthDao dao = handle.attach(AuthDao.class);
      return dao.getUserMemberships(SqlHelper.clear(email), UserStatus.JOINED.name());
    }
  }

  private boolean refreshAccessedAt(String hash) {
    try (Handle handle = Database.getHandle()) {
      AuthDao dao = handle.attach(AuthDao.class);
      return dao.refreshAccessedAt(hash);
    }
  }

}