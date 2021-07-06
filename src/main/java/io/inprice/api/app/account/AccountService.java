package io.inprice.api.app.account;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.account.dto.CreateDTO;
import io.inprice.api.app.account.dto.RegisterDTO;
import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.app.group.GroupDao;
import io.inprice.api.app.membership.MembershipDao;
import io.inprice.api.app.superuser.announce.AnnounceService;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.validator.EmailValidator;
import io.inprice.api.app.user.validator.PasswordValidator;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.helpers.Commons;
import io.inprice.api.helpers.PasswordHelper;
import io.inprice.api.info.Response;
import io.inprice.api.meta.RateLimiterType;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.token.TokenType;
import io.inprice.api.token.Tokens;
import io.inprice.api.utils.CurrencyFormats;
import io.inprice.common.config.SysProps;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.helpers.SqlHelper;
import io.inprice.common.info.EmailData;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.AppEnv;
import io.inprice.common.meta.EmailTemplate;
import io.inprice.common.meta.UserMarkType;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.User;
import io.inprice.common.models.UserMark;
import io.javalin.http.Context;

class AccountService {

  private static final Logger log = LoggerFactory.getLogger(AccountService.class);

  private final AnnounceService announceService = Beans.getSingleton(AnnounceService.class);
  private final RedisClient redis = Beans.getSingleton(RedisClient.class);

  Response requestRegistration(RegisterDTO dto) {
    Response res = redis.isEmailRequested(RateLimiterType.REGISTER, dto.getEmail());
    if (!res.isOK()) return res;

    res = validateRegisterDTO(dto);
    if (res.isOK()) {

      try (Handle handle = Database.getHandle()) {
    		AccountDao accountDao = handle.attach(AccountDao.class);
        UserMark um_BANNED = accountDao.getUserMarkByEmail(dto.getEmail(), UserMarkType.BANNED);
        
        if (um_BANNED == null) {
      		UserDao userDao = handle.attach(UserDao.class);
          
          User user = userDao.findByEmail(dto.getEmail());
          boolean isNotARegisteredUser = (user == null);
  
          if (isNotARegisteredUser) {
            String token = Tokens.add(TokenType.REGISTRATION_REQUEST, dto);
  
            Map<String, Object> mailMap = new HashMap<>(3);
            mailMap.put("user", dto.getEmail().split("@")[0]);
            mailMap.put("account", dto.getAccountName());
            mailMap.put("token", token.substring(0,3)+"-"+token.substring(3));
            
            if (!SysProps.APP_ENV.equals(AppEnv.PROD)) {
            	return new Response(mailMap);
            } else {
            	redis.sendEmail(
          			EmailData.builder()
            			.template(EmailTemplate.REGISTRATION_REQUEST)
            			.from(Props.APP_EMAIL_SENDER)
            			.to(dto.getEmail())
            			.subject("About " + dto.getAccountName() + " registration on inprice.io")
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
        log.error("Failed to render email for activating account register", e);
        return Responses.ServerProblem.EXCEPTION;
      }
    }
    return res;
  }

  Response completeRegistration(Context ctx, String token) {
    Response response = Responses.Invalid.TOKEN;

    RegisterDTO dto = Tokens.get(TokenType.REGISTRATION_REQUEST, token);
    if (dto != null) {
      Map<String, String> clientInfo = ClientSide.getGeoInfo(ctx.req);

      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        UserDao userDao = handle.attach(UserDao.class);

        User user = userDao.findByEmail(dto.getEmail());
        if (user == null) {
          user = new User();
          user.setEmail(dto.getEmail());
          user.setName(dto.getEmail().split("@")[0]);
          user.setTimezone(clientInfo.get(Consts.TIMEZONE));
          user.setCreatedAt(new Date());

          user.setId(
            userDao.insert(
              user.getEmail(), 
              PasswordHelper.getSaltedHash(dto.getPassword()), 
              user.getName(), 
              clientInfo.get(Consts.TIMEZONE)
            )
          );
        }
  
        if (user.getId() != null) {
          response = 
            createAccount(
              handle,
              user.getId(),
              user.getEmail(),
              dto.getAccountName(),
              clientInfo.get(Consts.CURRENCY_CODE),
              clientInfo.get(Consts.CURRENCY_FORMAT)
            );
        } else {
          response = Responses.NotFound.USER;
        }

        if (response.isOK()) {
          Map<String, Object> dataMap = new HashMap<>(2);
          dataMap.put("email", dto.getEmail());
          dataMap.put("account", dto.getAccountName());
          response = new Response(user);

          announceService.createWelcomeMsg(handle, user.getId());
          
          handle.commit();
          Tokens.remove(TokenType.REGISTRATION_REQUEST, token);
        } else {
        	handle.rollback();
        }
      }
    }

    return response;
  }

  Response getCurrentAccount() {
    try (Handle handle = Database.getHandle()) {
      AccountDao dao = handle.attach(AccountDao.class);
      return new Response(dao.findById(CurrentUser.getAccountId()));
    }
  }

  Response create(CreateDTO dto) {
    Response response = validateAccountDTO(dto);

    if (response.isOK()) {
      try (Handle handle = Database.getHandle()) {
      	handle.begin();

        response = 
          createAccount(
            handle,
            CurrentUser.getUserId(),
            CurrentUser.getEmail(),
            dto.getName(),
            dto.getCurrencyCode(),
            dto.getCurrencyFormat()
          );
        if (response.isOK()) {
          AccountDao accountDao = handle.attach(AccountDao.class);
          Account account = accountDao.findById(response.getData());
          response = Commons.refreshSession(account);
        }

        if (response.isOK())
        	handle.commit();
        else
        	handle.rollback();
      }
    }

    return response;
  }

  Response update(CreateDTO dto) {
    Response res = validateAccountDTO(dto);

    if (res.isOK()) {
      boolean isOK = false;

      try (Handle handle = Database.getHandle()) {
        AccountDao accountDao = handle.attach(AccountDao.class);
        isOK =
          accountDao.update(
            dto.getName(),
            dto.getCurrencyCode(),
            dto.getCurrencyFormat(),
            CurrentUser.getAccountId(),
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

  Response deleteAccount(String password) {
    Response response = Responses.DataProblem.DB_PROBLEM;

    try (Handle handle = Database.getHandle()) {
    	handle.begin();

      UserDao userDao = handle.attach(UserDao.class);
      AccountDao accountDao = handle.attach(AccountDao.class);
      UserSessionDao sessionDao = handle.attach(UserSessionDao.class);

      User user = userDao.findById(CurrentUser.getUserId());

      if (PasswordHelper.isValid(password, user.getPassword())) {
        Account account = accountDao.findByAdminId(CurrentUser.getUserId());

        if (account != null) {
          if (! AccountStatus.SUBSCRIBED.equals(account.getStatus())) {
            log.info("{} is being deleted. Id: {}...", account.getName(), account.getId());

            String where = "where account_id=" + CurrentUser.getAccountId();

            Batch batch = handle.createBatch();
            batch.add("SET FOREIGN_KEY_CHECKS=0");
            batch.add("delete from link_price " + where);
            batch.add("delete from link_history " + where);
            batch.add("delete from link_spec " + where);
            batch.add("delete from link " + where);
            batch.add("delete from link_group " + where);
            batch.add("delete from coupon where issued_id=" + CurrentUser.getAccountId() + " or issuer_id=" + CurrentUser.getAccountId());
            batch.add("delete from alarm " + where);
            batch.add("delete from ticket_history " + where);
            batch.add("delete from ticket_comment " + where);
            batch.add("delete from ticket " + where);
            batch.add("delete from announce_log " + where);
            batch.add("delete from announce " + where);
            batch.add("delete from access_log " + where);

            // in order to keep consistency, 
            // users having no account other than this must be deleted too!!!
            MembershipDao membershipDao = handle.attach(MembershipDao.class);
            List<Long> unboundMembers = membershipDao.findUserIdListHavingJustThisAccount(CurrentUser.getAccountId());
            if (unboundMembers != null && ! unboundMembers.isEmpty()) {
              String userIdList = StringUtils.join(unboundMembers, ",");
              batch.add("delete from user where id in (" + userIdList + ")");
            }
            
            batch.add("delete from membership " + where);
            batch.add("delete from user_session " + where);
            batch.add("delete from checkout " + where);
            batch.add("delete from account_history " + where);
            batch.add("delete from account_trans " + where);
            batch.add("delete from account where id=" + CurrentUser.getAccountId());

            batch.add("SET FOREIGN_KEY_CHECKS=1");
            batch.execute();

            List<String> hashList = sessionDao.findHashesByAccountId(CurrentUser.getAccountId());
            if (hashList != null && ! hashList.isEmpty()) {
              for (String hash : hashList) {
              	redis.removeSesion(hash);
              }
            }

            log.info("{} is deleted. Id: {}.", account.getName(), account.getId());
            response = Responses.OK;
          } else {
            response = Responses.Already.ACTIVE_SUBSCRIPTION;
          }
        } else {
          response = Responses.Invalid.ACCOUNT;
        }
      } else {
        response = Responses.Invalid.PASSWORD;
      }

      if (response.isOK())
      	handle.commit();
      else
      	handle.rollback();
    }

    return response;
  }

  private Response validateRegisterDTO(RegisterDTO dto) {
    Response res = validateAccountDTO(dto);

    if (res.isOK()) {
      String problem = EmailValidator.verify(dto.getEmail());

      if (problem == null) {
        PasswordDTO pswDTO = new PasswordDTO();
        pswDTO.setPassword(dto.getPassword());
        pswDTO.setRepeatPassword(dto.getRepeatPassword());
        problem = PasswordValidator.verify(pswDTO, true, false);
      }

      if (problem == null)
        return Responses.OK;
      else
        return new Response(problem);
    } else {
      return res;
    }
  }

  private Response createAccount(Handle handle, Long userId, String userEmail, String accountName, String currencyCode, String currencyFormat) {
    AccountDao accountDao = handle.attach(AccountDao.class);
    MembershipDao membershipDao = handle.attach(MembershipDao.class);
    GroupDao groupDao = handle.attach(GroupDao.class);

    Account account = accountDao.findByNameAndAdminId(accountName, userId);
    if (account == null) {
      Long accountId = 
        accountDao.insert(
          userId, 
          accountName,
          currencyCode,
          currencyFormat
        );

      if (accountId != null) {
        accountDao.insertStatusHistory(accountId, AccountStatus.CREATED);
        long memberId = 
          membershipDao.insert(
            userId,
            userEmail,
            accountId,
            UserRole.ADMIN,
            UserStatus.JOINED
          );

        if (memberId > 0) {
        	groupDao.insert("DEFAULT GROUP", BigDecimal.ZERO, accountId);
          log.info("A new user registered: {} - {} ", userEmail, accountName);
          return new Response(accountId);
        }
      }
      return Responses.DataProblem.DB_PROBLEM;
    } else {
      return Responses.Already.Defined.ACCOUNT;
    }
  }

  private Response validateAccountDTO(CreateDTO dto) {
    String problem = null;

    if (StringUtils.isBlank(dto.getName())) {
      problem = "Account name cannot be empty!";
    } else if (dto.getName().length() < 3 || dto.getName().length() > 70) {
      problem = "Account name must be between 3 - 70 chars";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCurrencyCode())) {
        problem = "Currency cannot be empty!";
      } else if (CurrencyFormats.get(dto.getCurrencyCode()) == null) {
        problem = "Unknown currency code!";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCurrencyFormat())) {
        problem = "Currency format cannot be empty!";
      } else {
        if (dto.getCurrencyFormat().length() < 3 || dto.getCurrencyFormat().length() > 16) {
          problem = "Currency format must be between 3 - 16 chars";
        } else {
          int count = StringUtils.countMatches(dto.getCurrencyFormat(), "#");
          if (count != 3) {
            problem = "Currency format is invalid";
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

  private Response validateAccountDTO(RegisterDTO dto) {
    String problem = EmailValidator.verify(dto.getEmail());

    if (problem == null) {
      if (StringUtils.isBlank(dto.getAccountName())) {
        problem = "Account name cannot be empty!";
      } else if (dto.getAccountName().length() < 3 || dto.getAccountName().length() > 70) {
        problem = "Account name must be between 3 - 70 chars!";
      }
    }

    if (problem == null) {
      //cleaning
      dto.setEmail(SqlHelper.clear(dto.getEmail()));
      dto.setAccountName(SqlHelper.clear(dto.getAccountName()));
      dto.setPassword(SqlHelper.clear(dto.getPassword()));
      dto.setRepeatPassword(SqlHelper.clear(dto.getRepeatPassword()));

      return Responses.OK;
    } else {
      return new Response(problem);
    }
  }

}
