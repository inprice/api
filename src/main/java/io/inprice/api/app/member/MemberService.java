package io.inprice.api.app.member;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.AccountDao;
import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.app.auth.dto.InvitationSendDTO;
import io.inprice.api.app.member.dto.InvitationUpdateDTO;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.user.validator.EmailValidator;
import io.inprice.api.consts.Responses;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.api.token.TokenType;
import io.inprice.api.token.Tokens;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.Member;
import io.inprice.common.models.User;

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
        User user = userDao.findByEmail(dto.getEmail());
        
        if (user == null || user.isBanned() == false) {
        	if (user == null || user.isPrivileged() == false) {

        		AccountDao accountDao = handle.attach(AccountDao.class);
        		Account account = accountDao.findById(CurrentUser.getAccountId());
        		if (account != null 
        				&& (account.getPlan() == null 
        				 || account.getPlan().getUserLimit().compareTo(account.getUserCount()) <= 0)) {
        			return Responses.PermissionProblem.USER_LIMIT_PROBLEM;
        		}

        		MemberDao memberDao = handle.attach(MemberDao.class);
            Member mem = memberDao.findByEmail(dto.getEmail(), CurrentUser.getAccountId());
            if (mem == null) {
            	
            	handle.begin();
            	
              boolean isAdded = memberDao.insertInvitation(dto.getEmail(), dto.getRole().name(), CurrentUser.getAccountId());
              if (isAdded) {
                boolean isOK = accountDao.increaseUserCount(CurrentUser.getAccountId());
                if (isOK) {
                	handle.commit();
                  dto.setAccountId(CurrentUser.getAccountId());
                  return sendMail(userDao, dto);
                } else {
                	handle.rollback();
                	return Responses.DataProblem.DB_PROBLEM;
                }
              }
            } else {
              return new Response("This user is already added to this account!");
            }
          } else {
          	return Responses.PermissionProblem.WRONG_USER;
          }
        } else {
        	return Responses.BANNED_USER;
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
        User user = userDao.findById(mem.getUserId());
      
        if (user == null || user.isBanned() == false) {
        	if (user == null || user.isPrivileged() == false) {
            boolean isOK = memberDao.increaseSendingCount(memId, UserStatus.PENDING.name(), CurrentUser.getAccountId());
            if (isOK) {
              InvitationSendDTO dto = new InvitationSendDTO();
              dto.setEmail(mem.getEmail());
              dto.setRole(mem.getRole());
              dto.setAccountId(CurrentUser.getAccountId());
              res = sendMail(userDao, dto);
            } else {
            	res = Responses.DataProblem.DB_PROBLEM;
            }
          } else {
          	res = Responses.PermissionProblem.WRONG_USER;
          }
        } else {
        	res = Responses.BANNED_USER;
        }
      } else {
      	res = Responses.NotFound.MEMBERSHIP;
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
        if (! mem.getAccountId().equals(CurrentUser.getAccountId())) {
          if (! mem.getStatus().equals(UserStatus.DELETED)) {
          	
          	handle.begin();
          	
            boolean isOK = memberDao.setStatusDeleted(memId, UserStatus.DELETED.name(), CurrentUser.getAccountId());
            if (isOK) {
            	terminateUserSession(handle, memId, CurrentUser.getAccountId());

            	handle.commit();
              res = Responses.OK;
            } else {
            	handle.rollback();
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
      Response res = Responses.NotFound.MEMBERSHIP;

    	try (Handle handle = Database.getHandle()) {
        MemberDao memberDao = handle.attach(MemberDao.class);

        Member mem = memberDao.findById(dto.getId());
        if (mem != null) {
          if (! mem.getAccountId().equals(CurrentUser.getAccountId()) && ! mem.getRole().equals(dto.getRole())) {
            if (! mem.getStatus().equals(UserStatus.DELETED)) {

            	handle.begin();
              
              boolean isOK = memberDao.changeRole(dto.getId(), dto.getRole().name(), CurrentUser.getAccountId());
              if (isOK) {
              	terminateUserSession(handle, dto.getId(), CurrentUser.getAccountId());
              	handle.commit();
                res = Responses.OK;
              } else {
              	handle.rollback();
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
    return new Response(problem);
  }

  Response pause(Long id) {
    Response res = Responses.NotFound.MEMBERSHIP;

    try (Handle handle = Database.getHandle()) {
      MemberDao memberDao = handle.attach(MemberDao.class);

      Member mem = memberDao.findById(id);
      if (mem != null) {
        if (! mem.getAccountId().equals(CurrentUser.getAccountId()) && ! mem.getStatus().equals(UserStatus.PAUSED)) {
          if (! mem.getStatus().equals(UserStatus.DELETED)) {

          	handle.begin();

            boolean isOK = memberDao.pause(id, CurrentUser.getAccountId());
            if (isOK) {
            	terminateUserSession(handle, id, CurrentUser.getAccountId());
            	handle.commit();
              res = Responses.OK;
            } else {
            	handle.rollback();
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

  Response resume(Long id) {
    Response res = Responses.NotFound.MEMBERSHIP;

    try (Handle handle = Database.getHandle()) {
      MemberDao memberDao = handle.attach(MemberDao.class);

      Member mem = memberDao.findById(id);
      if (mem != null) {
        if (! mem.getAccountId().equals(CurrentUser.getAccountId()) && mem.getStatus().equals(UserStatus.PAUSED)) {
          if (! mem.getStatus().equals(UserStatus.DELETED)) {

          	handle.begin();

            boolean isOK = memberDao.resume(id, CurrentUser.getAccountId());
            if (isOK) {
            	terminateUserSession(handle, id, CurrentUser.getAccountId());
            	handle.commit();
              res = Responses.OK;
            } else {
            	handle.rollback();
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
      dataMap.put("url", Props.APP_WEB_URL + "/accept-invitation");
      template = EmailTemplate.INVITATION_FOR_NEW_USERS;
    }

    message = renderer.render(template, dataMap);

    emailSender.send(Props.APP_EMAIL_SENDER,
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
  
  private void terminateUserSession(Handle handle, long userId, long accountId) {
    UserSessionDao userSessionDao = handle.attach(UserSessionDao.class);
    List<ForDatabase> dbSessions = userSessionDao.findListByUserId(userId);

    if (dbSessions != null && dbSessions.size() > 0) {
      List<String> hashList = new ArrayList<>(dbSessions.size());

      for (ForDatabase ses: dbSessions) {
      	if (ses.getAccountId().equals(accountId)) {
      		hashList.add(ses.getHash());
      	}
      }
      
      if (hashList.size() > 0) {
      	userSessionDao.deleteByHashList(hashList);
      	for (String hash : hashList) RedisClient.removeSesion(hash);
      }
    }
  }

}
