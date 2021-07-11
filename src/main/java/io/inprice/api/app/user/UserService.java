package io.inprice.api.app.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.app.membership.MembershipDao;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.dto.UserDTO;
import io.inprice.api.app.user.validator.PasswordValidator;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LongDTO;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.PasswordHelper;
import io.inprice.api.helpers.SessionHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForCookie;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.api.utils.Timezones;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.User;
import io.javalin.http.Context;

public class UserService {

  private final RedisClient redis = Beans.getSingleton(RedisClient.class);
	
  public Response updateInfo(UserDTO dto) {
    String problem = validateUserDTOForUpdate(dto);
    if (problem == null) {

      try (Handle handle = Database.getHandle()) {
        UserDao userDao = handle.attach(UserDao.class);

        boolean isOK = userDao.updateName(CurrentUser.getUserId(), dto.getName(), dto.getTimezone());
        if (isOK) {
          return Responses.OK;
        } else {
          return Responses.NotFound.USER;
        }
      }
    } else {
      return new Response(problem);
    }
  }

  public Response changePassword(PasswordDTO dto) {
  	Response res = Responses.NotFound.USER;

  	String problem = PasswordValidator.verify(dto);

  	if (problem == null) {
  		if (StringUtils.isBlank(dto.getOldPassword())) {
  			problem = "Old password cannot be empty!";
  		} else if (dto.getOldPassword().equals(dto.getPassword())) {
  			problem = "New password cannot be the same as old password!";
  		}
    }

  	if (problem == null) {

  		try (Handle handle = Database.getHandle()) {
      	UserDao userDao = handle.attach(UserDao.class);

        User user = userDao.findById(CurrentUser.getUserId());
        if (user != null) {
          if (PasswordHelper.isValid(dto.getOldPassword(), user.getPassword())) {

            String saltedHash = PasswordHelper.getSaltedHash(dto.getPassword());
            boolean isOK = userDao.updatePassword(CurrentUser.getUserId(), saltedHash);

            if (isOK) {
              res = Responses.OK;
            } else {
              res = Responses.DataProblem.DB_PROBLEM;
            }

          } else {	
          	res = new Response("Old password is incorrect!");
          }
        }
      }
    } else {
      res = new Response(problem);
    }

		return res;
  }

  public Response getInvitations() {
  	if (CurrentUser.getRole().equals(UserRole.SUPER)) return Responses.OK;

  	try (Handle handle = Database.getHandle()) {
      MembershipDao membershipDao = handle.attach(MembershipDao.class);
      return new Response(membershipDao.findMemberListByEmailAndStatus(CurrentUser.getEmail(), UserStatus.PENDING));
    }
  }

  public Response acceptInvitation(LongDTO dto) {
    if (dto.getValue() != null && dto.getValue() > 0) {
      return changeStatus(dto.getValue(), Arrays.asList(UserStatus.PENDING), UserStatus.JOINED);
    }
    return Responses.NotFound.USER;
  }

  public Response rejectInvitation(LongDTO dto) {
    if (dto.getValue() != null && dto.getValue() > 0) {
      return changeStatus(dto.getValue(), Arrays.asList(UserStatus.PENDING, UserStatus.PAUSED), UserStatus.REJECTED);
    }
    return Responses.NotFound.USER;
  }

  public Response leaveMembership(LongDTO dto) {
    if (dto.getValue() != null && dto.getValue() > 0) {
      return changeStatus(dto.getValue(), Arrays.asList(UserStatus.JOINED, UserStatus.PAUSED), UserStatus.LEFT);
    }
    return Responses.NotFound.USER;
  }

  private Response changeStatus(Long id, List<UserStatus> fromStatuses, UserStatus toStatus) {
    try (Handle handle = Database.getHandle()) {
      MembershipDao membershipDao = handle.attach(MembershipDao.class);

      boolean isOK = membershipDao.changeStatus(id, fromStatuses, toStatus, CurrentUser.getUserId());
      if (isOK) {
        return Responses.OK;
      } else {
        return Responses.NotFound.MEMBERSHIP;
      }
    }
  }

  public Response getMemberships() {
  	if (CurrentUser.getRole().equals(UserRole.SUPER)) return Responses.OK;

    try (Handle handle = Database.getHandle()) {
      MembershipDao membershipDao = handle.attach(MembershipDao.class);

      List<String> activeStatuses = new ArrayList<>(3);
      activeStatuses.add(UserStatus.JOINED.name());
      activeStatuses.add(UserStatus.PENDING.name());
      activeStatuses.add(UserStatus.PAUSED.name());

      return new Response(
        membershipDao.findMembershipsByEmail(CurrentUser.getEmail(), CurrentUser.getAccountId(), activeStatuses)
      );
    }
  }

  public Response getOpenedSessions(Context ctx) {
  	if (CurrentUser.getRole().equals(UserRole.SUPER)) return Responses.OK;

		String tokenString = ctx.cookie(Consts.SESSION);
		List<ForCookie> cookieSesList = SessionHelper.fromTokenForUser(tokenString);
  	
    List<String> excludedHashes = new ArrayList<>(cookieSesList.size());
    for (ForCookie ses: cookieSesList) {
      excludedHashes.add(ses.getHash());
    }

    if (excludedHashes.size() > 0) {
      try (Handle handle = Database.getHandle()) { 
        UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
        return new Response(
          userSessionDao.findOpenedSessions(CurrentUser.getUserId(), excludedHashes)
        );
      }
    }
    return Responses.NotFound.USER;
  }

  public Response closeAllSessions() {
    try (Handle handle = Database.getHandle()) {
      UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);

      List<ForDatabase> sessions = userSessionDao.findListByUserId(CurrentUser.getUserId());
      if (sessions != null && sessions.size() > 0) {
        for (ForDatabase ses : sessions) {
        	redis.removeSesion(ses.getHash());
        }
        if (userSessionDao.deleteByUserId(CurrentUser.getUserId())) {
          return Responses.OK;
        }
      }
    }
    return Responses.NotFound.USER;
  }

  private String validateUserDTOForUpdate(UserDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getName())) {
      problem = "User name cannot be empty!";
    } else if (dto.getName().length() < 3 || dto.getName().length() > 70) {
      problem = "User name must be between 3 - 70 chars!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getTimezone())) {
        problem = "Timezone cannot be empty!";
      } else if (!Timezones.exists(dto.getTimezone())) {
        problem = "Unknown timezone!";
      }
    }

    if (problem == null) {
      dto.setEmail(SqlHelper.clear(dto.getEmail()));
      dto.setName(SqlHelper.clear(dto.getName()));
    }

    return problem;
  }

}
