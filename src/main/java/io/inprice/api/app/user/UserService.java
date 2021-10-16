package io.inprice.api.app.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.app.membership.MembershipDao;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.dto.UserDTO;
import io.inprice.api.app.user.verifier.PasswordVerifier;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LongDTO;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.PasswordHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
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
	
  Response updateInfo(UserDTO dto) {
    String problem = validateUserDTOForUpdate(dto);
    if (problem == null) {

      try (Handle handle = Database.getHandle()) {
        UserDao userDao = handle.attach(UserDao.class);

        boolean isOK = userDao.updateInfo(CurrentUser.getUserId(), dto.getFullName(), dto.getTimezone());
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

  Response changePassword(PasswordDTO dto) {
  	Response res = Responses.NotFound.USER;

  	String problem = PasswordVerifier.verify(dto);

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

  Response getInvitations() {
  	if (CurrentUser.getRole().equals(UserRole.SUPER)) return Responses.OK;

  	try (Handle handle = Database.getHandle()) {
      MembershipDao membershipDao = handle.attach(MembershipDao.class);
      return new Response(membershipDao.findMemberListByEmailAndStatus(CurrentUser.getEmail(), UserStatus.PENDING));
    }
  }

  Response acceptInvitation(LongDTO dto) {
    if (dto.getValue() != null && dto.getValue() > 0) {
      return changeStatus(dto.getValue(), Arrays.asList(UserStatus.PENDING), UserStatus.JOINED);
    }
    return Responses.NotFound.USER;
  }

  Response rejectInvitation(LongDTO dto) {
    if (dto.getValue() != null && dto.getValue() > 0) {
      return changeStatus(dto.getValue(), Arrays.asList(UserStatus.PENDING, UserStatus.PAUSED), UserStatus.REJECTED);
    }
    return Responses.NotFound.USER;
  }

  Response leaveMembership(LongDTO dto) {
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

  Response getMemberships() {
  	if (CurrentUser.getRole().equals(UserRole.SUPER)) return Responses.OK;

    try (Handle handle = Database.getHandle()) {
      MembershipDao membershipDao = handle.attach(MembershipDao.class);

      List<String> activeStatuses = List.of(
	      UserStatus.JOINED.name(),
	      UserStatus.PENDING.name(),
	      UserStatus.PAUSED.name()
  		);

      return new Response(
        membershipDao.findMembershipsByEmail(CurrentUser.getEmail(), activeStatuses)
      );
    }
  }

  Response getOpenedSessions(Context ctx) {
  	if (CurrentUser.getRole().equals(UserRole.SUPER)) return Responses.OK;
    try (Handle handle = Database.getHandle()) { 
      UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
      List<ForDatabase> sessions = userSessionDao.findOpenedSessions(CurrentUser.getUserId());
      if (CurrentUser.getEmail().equals("demo@inprice.io")) {
      	for (ForDatabase ses: sessions) {
      		ses.setIp("HIDDEN");
      	}
      }
      return new Response(sessions);
    }
  }

  Response closeAllSessions() {
    try (Handle handle = Database.getHandle()) {
      UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);

      List<ForDatabase> sessions = userSessionDao.findListByUserId(CurrentUser.getUserId());
      if (CollectionUtils.isNotEmpty(sessions)) {
      	List<String> hashList = new ArrayList<>(sessions.size());
        for (ForDatabase ses : sessions) hashList.add(ses.getHash());
        redis.removeSesions(hashList);

        if (userSessionDao.deleteByUserId(CurrentUser.getUserId())) {
          return Responses.OK;
        }
      }
    }
    return Responses.NotFound.USER;
  }

  private String validateUserDTOForUpdate(UserDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getFullName())) {
      problem = "Full Name cannot be empty!";
    } else if (dto.getFullName().length() < 3 || dto.getFullName().length() > 70) {
      problem = "Full Name must be between 3 - 70 chars!";
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
      dto.setFullName(SqlHelper.clear(dto.getFullName()));
    }

    return problem;
  }

}
