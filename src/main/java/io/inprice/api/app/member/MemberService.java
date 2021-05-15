package io.inprice.api.app.member;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.auth.dto.InvitationSendDTO;
import io.inprice.api.app.member.dto.InvitationUpdateDTO;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.user.validator.EmailValidator;
import io.inprice.api.consts.Responses;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.token.TokenType;
import io.inprice.api.token.Tokens;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Member;

class MemberService {

  private static final Logger log = LoggerFactory.getLogger(MemberService.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);

  Response getList() {
    Response res = Responses.NotFound.MEMBERSHIP;

    try (Handle handle = Database.getHandle()) {
      MemberDao memberDao = handle.attach(MemberDao.class);

      List<Member> list = memberDao.findListByNotEmail(CurrentUser.getEmail(), CurrentUser.getAccountId());
      if (list != null && list.size() > 0) {
        res = new Response(list);
      }
    }
    return res;
  }

  Response invite(InvitationSendDTO dto) {
    String problem = validate(dto);
    if (problem == null) {

      try (Handle handle = Database.getHandle()) {
        UserDao userDao = handle.attach(UserDao.class);
        MemberDao memberDao = handle.attach(MemberDao.class);
  
        Member mem = memberDao.findByEmail(dto.getEmail(), CurrentUser.getAccountId());
        if (mem == null) {
          boolean isAdded = memberDao.insertInvitation(dto.getEmail(), dto.getRole().name(), CurrentUser.getAccountId());
          if (isAdded) {
            dto.setAccountId(CurrentUser.getAccountId());
            return sendMail(userDao, dto);
          }
        } else {
          return new Response("A user with " + dto.getEmail() + " address is already added to this account!");
        }
      }
    }
    return new Response(problem);
  }

  Response resend(long memId) {
    Response res = Responses.DataProblem.NOT_SUITABLE;

    try (Handle handle = Database.getHandle()) {
      UserDao userDao = handle.attach(UserDao.class);
      MemberDao memberDao = handle.attach(MemberDao.class);

      Member mem = memberDao.findById(memId);
      if (mem != null) {
        boolean isOK = memberDao.increaseSendingCount(memId, UserStatus.PENDING.name(), CurrentUser.getAccountId());
        if (isOK) {
          InvitationSendDTO dto = new InvitationSendDTO();
          dto.setEmail(mem.getEmail());
          dto.setRole(mem.getRole());
          dto.setAccountId(CurrentUser.getAccountId());
          res = sendMail(userDao, dto);
        }
      }
    }

    return res;
  }

  Response delete(long memId) {
    Response res = Responses.NotFound.MEMBERSHIP;

    try (Handle handle = Database.getHandle()) {
      MemberDao memberDao = handle.attach(MemberDao.class);

      Member mem = memberDao.findById(memId);
      if (mem != null) {
        if (mem.getAccountId().equals(CurrentUser.getAccountId())) {
          if (! mem.getStatus().equals(UserStatus.DELETED)) {
            boolean isOK = memberDao.setStatusDeleted(memId, UserStatus.DELETED.name(), CurrentUser.getAccountId());
            if (isOK) {
              res = Responses.OK;
            } else {
              res = Responses.DataProblem.DB_PROBLEM;
            }
          } else {
            res = Responses.Already.DELETED_MEMBER;
          }
        } else {
          res = Responses.DataProblem.NOT_SUITABLE;
        }
      } else {
        res = Responses.NotFound.MEMBERSHIP;
      }
    }
    return res;
  }

  Response changeRole(InvitationUpdateDTO dto) {
    String problem = validate(dto);

    if (problem == null) {
      try (Handle handle = Database.getHandle()) {
        MemberDao memberDao = handle.attach(MemberDao.class);
  
        boolean isOK = memberDao.changeRole(dto.getId(), dto.getRole().name(), CurrentUser.getAccountId());
        if (isOK) {
          return Responses.OK;
        } else {
          problem = Responses.DataProblem.DB_PROBLEM.getReason();
        }
      }
    }
    return new Response(problem);
  }

  Response pause(Long id) {
    try (Handle handle = Database.getHandle()) {
      MemberDao memberDao = handle.attach(MemberDao.class);

      boolean isOK = memberDao.pause(id, CurrentUser.getAccountId());
      if (isOK) {
        return Responses.OK;
      }
    }
    return Responses.DataProblem.DB_PROBLEM;
  }

  Response resume(Long id) {
    try (Handle handle = Database.getHandle()) {
      MemberDao memberDao = handle.attach(MemberDao.class);

      boolean isOK = memberDao.resume(id, CurrentUser.getAccountId());
      if (isOK) {
        return Responses.OK;
      }
    }
    return Responses.DataProblem.NOT_SUITABLE;
  }

  private Response sendMail(UserDao userDao, InvitationSendDTO dto) {
    Map<String, Object> dataMap = new HashMap<>(5);
    dataMap.put("account", CurrentUser.getAccountName());
    dataMap.put("admin", CurrentUser.getUserName());

    String message = null;
    EmailTemplate template = null;

    String userName = userDao.findUserNameByEmail(dto.getEmail());
    if (userName != null) {
      dataMap.put("user", userName);
      template = EmailTemplate.INVITATION_FOR_EXISTING_USERS;
    } else {
      dataMap.put("user", dto.getEmail().substring(0, dto.getEmail().indexOf('@')));
      dataMap.put("token", Tokens.add(TokenType.INVITATION, dto));
      dataMap.put("url", Props.APP_WEB_URL() + "/accept-invitation");
      template = EmailTemplate.INVITATION_FOR_NEW_USERS;
    }

    message = renderer.render(template, dataMap);

    emailSender.send(Props.APP_EMAIL_SENDER(),
        "About your invitation for " + CurrentUser.getAccountName() + " at inprice.io", dto.getEmail(), message);

    log.info("{} is invited as {} to {} ", dto.getEmail(), dto.getRole(), CurrentUser.getAccountId());
    log.info(message);
    return Responses.OK;
  }

  private String validate(InvitationSendDTO dto) {
    String problem = null;

    if (dto == null || (dto.getEmail() == null && !dto.getEmail().equalsIgnoreCase(CurrentUser.getEmail()))) {
      problem = Responses.Invalid.INVITATION.getReason();
    }

    if (problem == null && dto.getRole() == null || dto.getRole().equals(UserRole.ADMIN)) {
      problem = String.format("Role must be either %s or %s!", UserRole.EDITOR.name(), UserRole.VIEWER.name());
    }

    if (problem == null) {
      problem = EmailValidator.verify(dto.getEmail());
    }

    return problem;
  }

  private String validate(InvitationUpdateDTO dto) {
    String problem = null;

    if (dto == null || dto.getId() == null || dto.getId() < 1) {
      problem = "Invalid invitation data!";
    }

    if (dto.getRole() == null || dto.getRole().equals(UserRole.ADMIN)) {
      problem = String.format("Role must be either %s or %s!", UserRole.EDITOR.name(), UserRole.VIEWER.name());
    }

    return problem;
  }

}
