package io.inprice.api.app.membership;

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
import io.inprice.api.app.membership.dto.InvitationUpdateDTO;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.user.validator.EmailValidator;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.session.info.ForDatabase;
import io.inprice.api.token.TokenType;
import io.inprice.api.token.Tokens;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;

class MembershipService {

  private static final Logger log = LoggerFactory.getLogger(MembershipService.class);

  private final RedisClient redis = Beans.getSingleton(RedisClient.class);

  Response getList() {
    Response res = Responses.NotFound.MEMBERSHIP;

    try (Handle handle = Database.getHandle()) {
      MembershipDao membershipDao = handle.attach(MembershipDao.class);

      List<Membership> list = membershipDao.findNormalMemberList(CurrentUser.getAccountId());
      if (list != null && list.size() > 0) {
        res = new Response(list);
      }
    }
    return res;
  }

  Response invite(InvitationSendDTO dto) {
    String problem = validate(dto);
    if (problem == null) {

    	if (CurrentUser.getEmail().equalsIgnoreCase(dto.getEmail()) == false) {
    	
        try (Handle handle = Database.getHandle()) {
          UserDao userDao = handle.attach(UserDao.class);
          User user = userDao.findByEmail(dto.getEmail());
          
          if (user == null || user.isBanned() == false) {
          	if (user == null || user.isPrivileged() == false) {
  
          		AccountDao accountDao = handle.attach(AccountDao.class);
          		Account account = accountDao.findById(CurrentUser.getAccountId());
          		if (account != null) {
          			if (account.getPlan() == null) {
          				return Responses.PermissionProblem.DONT_HAVE_A_PLAN;
          			} else if (account.getPlan().getUserLimit().compareTo(account.getUserCount()) <= 0) {
          				return Responses.PermissionProblem.USER_LIMIT_PROBLEM;
          			}
          		}
  
          		MembershipDao membershipDao = handle.attach(MembershipDao.class);
              Membership mem = membershipDao.findByEmail(dto.getEmail(), CurrentUser.getAccountId());
              if (mem == null) {
              	
              	handle.begin();
  
              	//TODO: an announce must be fired here!
                boolean isAdded = membershipDao.insertInvitation(dto.getEmail(), dto.getRole(), CurrentUser.getAccountId());
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
                return new Response("This user has already been added to this account!");
              }
            } else {
            	return Responses.PermissionProblem.WRONG_USER;
            }
          } else {
          	return Responses.BANNED_USER;
          }
        }
    	} else {
    		return Responses.METHOD_NOT_ALLOWED;
      }
    }
    return new Response(problem);
  }

  Response resend(long memId) {
    Response res = Responses.NotFound.MEMBERSHIP;

    try (Handle handle = Database.getHandle()) {
      UserDao userDao = handle.attach(UserDao.class);
      MembershipDao membershipDao = handle.attach(MembershipDao.class);
      
      Membership mem = membershipDao.findNormalMemberById(memId, CurrentUser.getAccountId());
      if (mem != null) {
      	if (mem.getUserId() == null || mem.getUserId().equals(CurrentUser.getUserId()) == false) {

      		if (UserStatus.PENDING.equals(mem.getStatus())) {
            User user = userDao.findById(mem.getUserId());
    
            if (user == null || (user.isBanned() == false && user.isPrivileged() == false)) {
              boolean isOK = membershipDao.increaseSendingCount(memId, UserStatus.PENDING, CurrentUser.getAccountId());
              if (isOK) {
                InvitationSendDTO dto = new InvitationSendDTO();
                dto.setEmail(mem.getEmail());
                dto.setRole(mem.getRole());
                dto.setAccountId(CurrentUser.getAccountId());
                res = sendMail(userDao, dto);
              } else {
              	res = new Response("You can re-send invitation for the same user up to three times!");
              }
            } else {
            	res = Responses.BANNED_USER;
            }
          } else {
          	res = new Response("You cannot re-send an invitation since this user is not in PENDING status!");
          }
      	} else {
      		res = Responses.METHOD_NOT_ALLOWED;
        }
      }
  	}

    return res;
  }

  Response delete(long memId) {
    Response res = Responses.NotFound.MEMBERSHIP;

    try (Handle handle = Database.getHandle()) {
      MembershipDao membershipDao = handle.attach(MembershipDao.class);

      Membership mem = membershipDao.findNormalMemberById(memId, CurrentUser.getAccountId());
      if (mem != null) {

      	if (mem.getUserId().equals(CurrentUser.getUserId()) == false) {
        	res = Responses.Already.DELETED_MEMBER;
  
        	if (! mem.getStatus().equals(UserStatus.DELETED)) {
          	handle.begin();
          	
            boolean isOK = membershipDao.setStatusDeleted(memId, UserStatus.DELETED, CurrentUser.getAccountId());
            if (isOK) {
            	if (mem.getUserId() != null) {
            		terminateUserSession(handle, mem.getUserId(), CurrentUser.getAccountId());
            	}
  
            	handle.commit();
              res = Responses.OK;
            } else {
            	handle.rollback();
            }
          }
      	} else {
      		res = Responses.METHOD_NOT_ALLOWED;
      	}
      }
    }
    return res;
  }

  Response changeRole(InvitationUpdateDTO dto) {
    String problem = validate(dto);

    if (problem == null) {
      Response res = Responses.NotFound.MEMBERSHIP;

    	try (Handle handle = Database.getHandle()) {
        MembershipDao membershipDao = handle.attach(MembershipDao.class);
        Membership mem = membershipDao.findNormalMemberById(dto.getMemberId(), CurrentUser.getAccountId());

    		if (mem != null) {
        	if (mem.getUserId().equals(CurrentUser.getUserId()) == false) {
          
        		if (! mem.getStatus().equals(UserStatus.DELETED)) {
        			if (! mem.getRole().equals(dto.getRole())) {
  
              	handle.begin();

                boolean isOK = membershipDao.changeRole(dto.getMemberId(), dto.getRole(), CurrentUser.getAccountId());
                if (isOK) {
                	if (mem.getUserId() != null) {
                		terminateUserSession(handle, mem.getUserId(), CurrentUser.getAccountId());
                	}

                	handle.commit();
                  res = Responses.OK;
                } else {
                	handle.rollback();
                  res = Responses.DataProblem.DB_PROBLEM;
                }
              } else {
                res = Responses.DataProblem.NOT_SUITABLE;
              }
            } else {
              res = Responses.Already.DELETED_MEMBER;
            }
        	} else {
        		res = Responses.METHOD_NOT_ALLOWED;
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
      MembershipDao membershipDao = handle.attach(MembershipDao.class);

      Membership mem = membershipDao.findNormalMemberById(id, CurrentUser.getAccountId());
      if (mem != null) {
      	if (mem.getUserId().equals(CurrentUser.getUserId()) == false) {

        	if (! mem.getStatus().equals(UserStatus.DELETED)) {
        		if (! mem.getStatus().equals(UserStatus.PAUSED)) {
  
            	handle.begin();
  
              boolean isOK = membershipDao.pause(id, CurrentUser.getAccountId());
              if (isOK) {
              	if (mem.getUserId() != null) {
              		terminateUserSession(handle, mem.getUserId(), CurrentUser.getAccountId());
              	}
  
              	handle.commit();
                res = Responses.OK;
              } else {
              	handle.rollback();
                res = Responses.DataProblem.DB_PROBLEM;
              }
            } else {
              res = Responses.Already.PAUSED_MEMBER;
            }
          } else {
            res = Responses.Already.DELETED_MEMBER;
          }
        } else {
      		res = Responses.METHOD_NOT_ALLOWED;
        }
      }
    }
	
    return res;
  }

  Response resume(Long id) {
    Response res = Responses.NotFound.MEMBERSHIP;

    try (Handle handle = Database.getHandle()) {
      MembershipDao membershipDao = handle.attach(MembershipDao.class);

      Membership mem = membershipDao.findNormalMemberById(id, CurrentUser.getAccountId());
      if (mem != null) {
      	if (mem.getUserId().equals(CurrentUser.getUserId()) == false) {

        	if (! mem.getStatus().equals(UserStatus.DELETED)) {
          	if (mem.getStatus().equals(UserStatus.PAUSED)) {
  
            	handle.begin();
  
              boolean isOK = membershipDao.resume(id, CurrentUser.getAccountId());
              if (isOK) {
              	if (mem.getUserId() != null) {
              		terminateUserSession(handle, mem.getUserId(), CurrentUser.getAccountId());
              	}
  
              	handle.commit();
                res = Responses.OK;
              } else {
              	handle.rollback();
                res = Responses.DataProblem.DB_PROBLEM;
              }
            } else {
            	res = new Response("This member is not paused!");
            }
          } else {
            res = Responses.Already.DELETED_MEMBER;
          }
        } else {
      		res = Responses.METHOD_NOT_ALLOWED;
        }
      } else {
        res = Responses.NotFound.MEMBERSHIP;
      }
    }
	
    return res;
  }

  private Response sendMail(UserDao userDao, InvitationSendDTO dto) {
    Map<String, Object> mailMap = new HashMap<>(5);
    mailMap.put("account", CurrentUser.getAccountName());
    mailMap.put("admin", CurrentUser.getUserName());

    EmailTemplate template = null;

    String userName = userDao.findUserNameByEmail(dto.getEmail());
    if (userName != null) {
      mailMap.put("user", userName);
      template = EmailTemplate.INVITATION_FOR_EXISTING_USERS;
    } else {
      mailMap.put("user", dto.getEmail().substring(0, dto.getEmail().indexOf('@')));
      mailMap.put("token", Tokens.add(TokenType.INVITATION, dto));
      mailMap.put("url", Props.APP_WEB_URL + "/accept-invitation");
      template = EmailTemplate.INVITATION_FOR_NEW_USERS;
    }

    redis.sendEmail(
			EmailData.builder()
  			.template(template)
  			.from(Props.APP_EMAIL_SENDER)
  			.to(dto.getEmail())
  			.subject("About your invitation for " + CurrentUser.getAccountName() + " at inprice.io")
  			.data(mailMap)
  		.build()	
		);

    log.info("{} is invited as {} to {} ", dto.getEmail(), dto.getRole(), CurrentUser.getAccountId());

    if (AppEnv.TEST.equals(SysProps.APP_ENV)) {
    	return new Response(mailMap);
    } else {
    	return Responses.OK;
    }
  }

  private String validate(InvitationSendDTO dto) {
    String problem = EmailValidator.verify(dto.getEmail());
    
    if (problem == null && dto.getEmail().equalsIgnoreCase(CurrentUser.getEmail())) {
    	problem = "You cannot invite yourself!";
    }

    if (problem == null && (dto.getRole() == null || dto.getRole().equals(UserRole.ADMIN) || dto.getRole().equals(UserRole.SUPER))) {
      problem = String.format("Role must be either %s or %s!", UserRole.EDITOR, UserRole.VIEWER);
    }

    return problem;
  }

  private String validate(InvitationUpdateDTO dto) {
    String problem = null;

    if (dto.getMemberId() == null || dto.getMemberId() < 1) {
      problem = "Invalid member id!";
    }

    if (dto.getRole() == null || dto.getRole().equals(UserRole.ADMIN) || dto.getRole().equals(UserRole.SUPER)) {
      problem = String.format("Role must be either %s or %s!", UserRole.EDITOR, UserRole.VIEWER);
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
      	for (String hash : hashList) redis.removeSesion(hash);
      }
    }
  }

}
