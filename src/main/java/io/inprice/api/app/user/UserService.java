package io.inprice.api.app.user;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;

import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.app.member.MemberDao;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.dto.UserDTO;
import io.inprice.api.app.user.validator.PasswordValidator;
import io.inprice.api.consts.Responses;
import io.inprice.api.dto.LongDTO;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.PasswordHelper;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForCookie;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.api.utils.Timezones;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;

public class UserService {

  public Response update(UserDTO dto) {
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

  public Response updatePassword(PasswordDTO dto) {
		dto.setId(CurrentUser.getUserId());

		String problem = PasswordValidator.verify(dto, true, true);
    if (problem == null) {

      try (Handle handle = Database.getHandle()) {
        UserDao userDao = handle.attach(UserDao.class);

        String saltedHash = PasswordHelper.getSaltedHash(dto.getPassword());
        boolean isOK = userDao.updatePassword(CurrentUser.getUserId(), saltedHash);

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

  public Response getInvitations() {
    try (Handle handle = Database.getHandle()) {
      MemberDao memberDao = handle.attach(MemberDao.class);
      return new Response(memberDao.findMemberListByEmailAndStatus(CurrentUser.getEmail(), UserStatus.PENDING.name()));
    }
  }

  public Response acceptInvitation(LongDTO dto) {
    if (dto.getValue() != null && dto.getValue() > 0) {
      return processInvitation(dto.getValue(), UserStatus.PENDING, UserStatus.JOINED);
    }
    return Responses.NotFound.USER;
  }

  public Response rejectInvitation(LongDTO dto) {
    if (dto.getValue() != null && dto.getValue() > 0) {
      return processInvitation(dto.getValue(), UserStatus.JOINED, UserStatus.LEFT);
    }
    return Responses.NotFound.USER;
  }

  private Response processInvitation(Long id, UserStatus fromStatus, UserStatus toStatus) {
    try (Handle handle = Database.getHandle()) {
      MemberDao memberDao = handle.attach(MemberDao.class);

      boolean isOK = memberDao.changeStatus(id, fromStatus.name(), toStatus.name(), CurrentUser.getUserId());
      if (isOK) {
        return Responses.OK;
      } else {
        return Responses.NotFound.USER;
      }
    }
  }

  public Response getMemberships() {
    try (Handle handle = Database.getHandle()) {
      MemberDao memberDao = handle.attach(MemberDao.class);

      List<String> statuses = new ArrayList<>(3);
      statuses.add(UserStatus.JOINED.name());
      statuses.add(UserStatus.PENDING.name());
      statuses.add(UserStatus.PAUSED.name());

      return new Response(
        memberDao.findMembershipsByEmail(CurrentUser.getEmail(), CurrentUser.getAccountId(), statuses)
      );
    }
  }

  public Response leaveMember(LongDTO dto) {
    if (dto.getValue() != null && dto.getValue() > 0) {
      try (Handle handle = Database.getHandle()) {
        MemberDao memberDao = handle.attach(MemberDao.class);
        boolean isOK = memberDao.changeStatus(dto.getValue(), UserStatus.LEFT.name());
        if (isOK) {
          return Responses.OK;
        }
      }
    }
    return Responses.NotFound.USER;
  }

  public Response getOpenedSessions(List<ForCookie> cookieSesList) {
  	if (CurrentUser.getRole().equals(UserRole.SUPER)) return Responses.OK;

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
          RedisClient.removeSesion(ses.getHash());
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

    if (dto == null) {
      problem = "Invalid user info!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getName())) {
        problem = "User name cannot be empty!";
      } else if (dto.getName().length() < 3 || dto.getName().length() > 70) {
        problem = "User name must be between 3 - 70 chars";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getTimezone())) {
        problem = "Time zone cannot be empty!";
      } else if (!Timezones.exists(dto.getTimezone())) {
        problem = "Unknown time zone!";
      }
    }

    if (problem == null) {
      dto.setEmail(SqlHelper.clear(dto.getEmail()));
      dto.setName(SqlHelper.clear(dto.getName()));
    }

    return problem;
  }

}
