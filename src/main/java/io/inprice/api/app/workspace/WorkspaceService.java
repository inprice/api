package io.inprice.api.app.workspace;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.app.membership.MembershipDao;
import io.inprice.api.app.superuser.announce.AnnounceService;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.verifier.EmailVerifier;
import io.inprice.api.app.user.verifier.PasswordVerifier;
import io.inprice.api.app.workspace.dto.CreateDTO;
import io.inprice.api.app.workspace.dto.RegisterDTO;
import io.inprice.api.config.Props;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.helpers.Commons;
import io.inprice.api.helpers.PasswordHelper;
import io.inprice.api.info.Response;
import io.inprice.api.meta.RateLimiterType;
import io.inprice.api.publisher.EmailPublisher;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.token.TokenType;
import io.inprice.api.token.Tokens;
import io.inprice.api.utils.CurrencyFormats;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.meta.Marks;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.meta.WorkspaceStatus;
import io.inprice.common.models.User;
import io.inprice.common.models.UserMarks;
import io.inprice.common.models.Workspace;
import io.javalin.http.Context;

class WorkspaceService {

  private static final Logger logger = LoggerFactory.getLogger(WorkspaceService.class);

  private final AnnounceService announceService = Beans.getSingleton(AnnounceService.class);
  private final RedisClient redis = Beans.getSingleton(RedisClient.class);

  Response requestRegistration(RegisterDTO dto) {
    Response res = redis.isEmailRequested(RateLimiterType.REGISTER, dto.getEmail());
    if (res.isOK() == false) return res;

    res = validateRegisterDTO(dto);
    if (res.isOK()) {

      try (Handle handle = Database.getHandle()) {
    		WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
        UserMarks um_BANNED = workspaceDao.findUserMarkByEmail(dto.getEmail(), Marks.BANNED.name());
        
        if (um_BANNED == null) {
      		UserDao userDao = handle.attach(UserDao.class);
          
          User user = userDao.findByEmail(dto.getEmail());
          boolean isNotARegisteredUser = (user == null);
  
          if (isNotARegisteredUser) {
            String token = Tokens.add(TokenType.REGISTRATION_REQUEST, dto);
  
            Map<String, Object> mailMap = Map.of(
            	"fullName", dto.getFullName(),
            	"workspaceName", dto.getWorkspaceName(),
            	"token", token.substring(0,3)+"-"+token.substring(3),
            	"url", Props.getConfig().APP.WEB_URL + Consts.Paths.Auth.COMPLETE_REGISTRATION
          	);

            if (Props.getConfig().APP.ENV.equals(Consts.Env.TEST)) {
            	return new Response(mailMap);
            } else {            
	          	EmailPublisher.publish(
	        			EmailData.builder()
	          			.template(EmailTemplate.REGISTRATION_REQUEST)
	          			.to(dto.getEmail())
	          			.subject("About " + dto.getWorkspaceName() + " registration on inprice.io")
	          			.data(mailMap)
	          		.build()	
	    				);
	          	redis.removeRequestedEmail(RateLimiterType.REGISTER, dto.getEmail());
	            return Responses.OK;
            }
          } else {
            return Responses.Already.Defined.REGISTERED_USER;
          }

        } else {
        	return Responses.BANNED_USER;
        }

      } catch (Exception e) {
        logger.error("Failed to render email for activating workspace register", e);
        return Responses.ServerProblem.EXCEPTION;
      }
    }
    return res;
  }

  Response completeRegistration(Context ctx, String token) {
    Response response = Responses.Invalid.TOKEN;

    RegisterDTO dto = Tokens.get(TokenType.REGISTRATION_REQUEST, token, RegisterDTO.class);
    if (dto != null) {
      Map<String, String> clientInfo = ClientSide.getGeoInfo(ctx.req);

      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        UserDao userDao = handle.attach(UserDao.class);

        User user = userDao.findByEmail(dto.getEmail());
        if (user == null) {
          user = new User();
          user.setFullName(dto.getFullName());
          user.setEmail(dto.getEmail());
          user.setTimezone(clientInfo.get(Consts.TIMEZONE));
          user.setCreatedAt(new Date());

          user.setId(
            userDao.insert(
          		user.getFullName(), 
              user.getEmail(), 
              PasswordHelper.getSaltedHash(dto.getPassword()), 
              clientInfo.get(Consts.TIMEZONE)
            )
          );
        }
  
        if (user.getId() != null) {
          response = 
            createWorkspace(
              handle,
              user.getId(),
              user.getEmail(),
              dto.getWorkspaceName(),
              clientInfo.get(Consts.CURRENCY_CODE),
              clientInfo.get(Consts.CURRENCY_FORMAT)
            );
        } else {
          response = Responses.NotFound.USER;
        }

        if (response.isOK()) {
          response = new Response(user);
          announceService.createWelcomeMsg(handle, user.getId());
          handle.commit();

          Map<String, Object> mailMap = Map.of(
          	"fullName", dto.getFullName(),
          	"workspaceName", dto.getWorkspaceName()
        	);
        	EmailPublisher.publish(
      			EmailData.builder()
        			.template(EmailTemplate.REGISTRATION_COMPLETE)
        			.to(dto.getEmail())
        			.subject(dto.getWorkspaceName() + " is active.")
        			.data(mailMap)
        		.build()	
  				);
          
          Tokens.remove(TokenType.REGISTRATION_REQUEST, token);
        } else {
        	handle.rollback();
        }
      }
    }

    return response;
  }

  Response getCurrentWorkspace() {
    try (Handle handle = Database.getHandle()) {
      WorkspaceDao dao = handle.attach(WorkspaceDao.class);
      return new Response(dao.findById(CurrentUser.getWorkspaceId()));
    }
  }

  Response create(CreateDTO dto) {
    Response response = validateWorkspaceDTO(dto);

    if (response.isOK()) {
      try (Handle handle = Database.getHandle()) {
      	WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);

      	int wsCount = workspaceDao.findWorkspaceCount(CurrentUser.getUserId());
        if (wsCount < 5) {
	      	handle.begin();
	
	        response = 
	          createWorkspace(
	            handle,
	            CurrentUser.getUserId(),
	            CurrentUser.getEmail(),
	            dto.getName(),
	            dto.getCurrencyCode(),
	            dto.getCurrencyFormat()
	          );
	        if (response.isOK()) {
	          Workspace workspace = workspaceDao.findById(response.getData());
	          response = Commons.refreshSession(workspace);
	        }
	
	        if (response.isOK())
	        	handle.commit();
	        else
	        	handle.rollback();
        } else {
        	response = new Response("You can create up to 5 workspaces!");
        }
      }
    }

    return response;
  }

  Response update(CreateDTO dto) {
    Response res = validateWorkspaceDTO(dto);

    if (res.isOK()) {
      boolean isOK = false;

      try (Handle handle = Database.getHandle()) {
        WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
        isOK =
          workspaceDao.update(
            dto.getName(),
            dto.getCurrencyCode(),
            dto.getCurrencyFormat(),
            CurrentUser.getWorkspaceId(),
            CurrentUser.getUserId()
          );
      }
  
      if (isOK) {
        res = Responses.OK;
      } else {
        res = Responses.DataProblem.DB_PROBLEM;
      }
    }
    return res;
  }

  Response deleteWorkspace(String password) {
  	Response res = Responses.Invalid.PASSWORD;

  	if (StringUtils.isNotBlank(password) && password.length() > 3 && password.length() < 17) {

      try (Handle handle = Database.getHandle()) {
      	handle.begin();
  
        UserDao userDao = handle.attach(UserDao.class);
        WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
        UserSessionDao sessionDao = handle.attach(UserSessionDao.class);
  
        User user = userDao.findById(CurrentUser.getUserId());
  
        if (PasswordHelper.isValid(password, user.getPassword())) {
          Workspace workspace = workspaceDao.findByAdminId(CurrentUser.getUserId());

          Long baseLimit = 2L;
          if (Props.getConfig().APP.ENV.equals(Consts.Env.TEST)) baseLimit = 0L;

          if (workspace != null && workspace.getId() > baseLimit) {
            logger.info("{} is being deleted. Id: {}...", workspace.getName(), workspace.getId());
            
            List<String> hashList = sessionDao.findHashesByWorkspaceId(CurrentUser.getWorkspaceId());
            if (hashList != null && ! hashList.isEmpty()) {
            	redis.removeSesions(hashList);
            }

            String where = "where workspace_id=" + CurrentUser.getWorkspaceId();

            Batch batch = handle.createBatch();
            batch.add("SET FOREIGN_KEY_CHECKS=0");
            batch.add("delete from link_price " + where);
            batch.add("delete from link_history " + where);
            batch.add("delete from link_spec " + where);
            batch.add("delete from link " + where);
            batch.add("delete from product " + where);
            batch.add("delete from brand " + where);
            batch.add("delete from category " + where);
            batch.add("delete from alarm " + where);
            batch.add("delete from voucher where issued_id=" + CurrentUser.getWorkspaceId() + " or issuer_id=" + CurrentUser.getWorkspaceId());
            batch.add("delete from ticket_history " + where);
            batch.add("delete from ticket_comment " + where);
            batch.add("delete from ticket " + where);
            batch.add("delete from announce_log " + where);
            batch.add("delete from announce " + where);

            // in order to keep consistency, 
            // users having no workspace other than this must be deleted too!!!
            MembershipDao membershipDao = handle.attach(MembershipDao.class);
            List<Long> unboundMembers = membershipDao.findUserIdListHavingJustThisWorkspace(CurrentUser.getWorkspaceId());
            if (unboundMembers != null && ! unboundMembers.isEmpty()) {
              String userIdList = StringUtils.join(unboundMembers, ",");
              batch.add("delete from user where id in (" + userIdList + ")");
            }
            
            batch.add("delete from membership " + where);
            batch.add("delete from user_session " + where);
            batch.add("delete from checkout " + where);
            batch.add("delete from workspace_history " + where);
            batch.add("delete from workspace_trans " + where);
            batch.add("delete from workspace where id=" + CurrentUser.getWorkspaceId());

            batch.add("SET FOREIGN_KEY_CHECKS=1");
            batch.execute();

            logger.info("{} is deleted. Id: {}.", workspace.getName(), workspace.getId());
            res = Responses.OK;
          } else {
            res = Responses.Invalid.WORKSPACE;
          }
        }
  
        if (res.isOK())
        	handle.commit();
        else
        	handle.rollback();
      }
  	}

    return res;
  }

  private Response validateRegisterDTO(RegisterDTO dto) {
    Response res = validateWorkspaceDTO(dto);

    if (res.isOK()) {
      String problem = EmailVerifier.verify(dto.getEmail());

      if (problem == null) {
        PasswordDTO pswDTO = new PasswordDTO();
        pswDTO.setPassword(dto.getPassword());
        pswDTO.setRepeatPassword(dto.getRepeatPassword());
        problem = PasswordVerifier.verify(pswDTO);
      }

      if (problem == null)
        return Responses.OK;
      else
        return new Response(problem);
    } else {
      return res;
    }
  }

  private Response createWorkspace(Handle handle, Long userId, String userEmail, String workspaceName, String currencyCode, String currencyFormat) {
    WorkspaceDao workspaceDao = handle.attach(WorkspaceDao.class);
    MembershipDao membershipDao = handle.attach(MembershipDao.class);

    Workspace workspace = workspaceDao.findByNameAndAdminId(workspaceName, userId);
    if (workspace == null) {
      Long workspaceId = 
        workspaceDao.insert(
          userId, 
          workspaceName,
          currencyCode,
          currencyFormat
        );

      if (workspaceId != null) {
        workspaceDao.insertStatusHistory(workspaceId, WorkspaceStatus.CREATED);
        long memberId = 
          membershipDao.insert(
            userId,
            userEmail,
            workspaceId,
            UserRole.ADMIN,
            UserStatus.JOINED
          );

        if (memberId > 0) {
          logger.info("A new user registered: {} - {} ", userEmail, workspaceName);
          return new Response(workspaceId);
        }
      }
      return Responses.DataProblem.DB_PROBLEM;
    } else {
      return Responses.Already.Defined.WORKSPACE;
    }
  }

  private Response validateWorkspaceDTO(CreateDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getName())) {
      problem = "Workspace name cannot be empty!";
    } else if (dto.getName().length() < 5 || dto.getName().length() > 70) {
      problem = "Workspace name must be between 5 - 70 chars!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCurrencyCode())) {
        problem = "Currency code cannot be empty!";
      } else if (dto.getCurrencyCode().length() != 3) {
        problem = "Currency code must be 3 chars!";
      } else if (CurrencyFormats.get(dto.getCurrencyCode()) == null) {
        problem = "Unknown currency code!";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCurrencyFormat())) {
        problem = "Currency format cannot be empty!";
      } else {
        if (dto.getCurrencyFormat().length() < 3 || dto.getCurrencyFormat().length() > 16) {
          problem = "Currency format must be between 3 - 16 chars!";
        } else {
          int count = StringUtils.countMatches(dto.getCurrencyFormat(), "#");
          if (count != 3) {
            problem = "Currency format is invalid! Example: $#,##0.00";
          }
        }
      }
    }

    if (problem == null) {
      //cleaning
      dto.setName(SqlHelper.clear(dto.getName()));
      dto.setCurrencyCode(SqlHelper.clear(dto.getCurrencyCode()));
      dto.setCurrencyFormat(SqlHelper.clear(dto.getCurrencyFormat()));

      return Responses.OK;
    } else {
      return new Response(problem);
    }
  }

  private Response validateWorkspaceDTO(RegisterDTO dto) {
    String problem = EmailVerifier.verify(dto.getEmail());

    if (problem == null) {
    	if (StringUtils.isBlank(dto.getFullName())) {
    		problem = "Full Name cannot be empty!";
    	} else if (dto.getFullName().length() < 3 || dto.getFullName().length() > 70) {
    		problem = "Full Name must be between 3 - 70 chars!";
    	}
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getWorkspaceName())) {
        problem = "Workspace name cannot be empty!";
      } else if (dto.getWorkspaceName().length() < 3 || dto.getWorkspaceName().length() > 70) {
        problem = "Workspace name must be between 3 - 70 chars!";
      }
    }

    if (problem == null) {
      //cleaning
      dto.setEmail(SqlHelper.clear(dto.getEmail()));
      dto.setWorkspaceName(SqlHelper.clear(dto.getWorkspaceName()));
      dto.setPassword(SqlHelper.clear(dto.getPassword()));
      dto.setRepeatPassword(SqlHelper.clear(dto.getRepeatPassword()));

      return Responses.OK;
    } else {
      return new Response(problem);
    }
  }

}
