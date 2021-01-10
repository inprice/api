package io.inprice.api.app.account;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Batch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.auth.UserSessionDao;
import io.inprice.api.app.account.dto.CreateDTO;
import io.inprice.api.app.account.dto.RegisterDTO;
import io.inprice.api.app.member.MemberDao;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.validator.EmailValidator;
import io.inprice.api.app.user.validator.PasswordValidator;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.EmailTemplate;
import io.inprice.api.email.TemplateRenderer;
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
import io.inprice.common.meta.AppEnv;
import io.inprice.common.meta.AccountStatus;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Account;
import io.inprice.common.models.User;
import io.javalin.http.Context;

class AccountService {

  private static final Logger log = LoggerFactory.getLogger(AccountService.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);

  Response requestRegistration(RegisterDTO dto) {
    Response res = Responses.OK;
    if (SysProps.APP_ENV().equals(AppEnv.PROD)) {
      res = RedisClient.isEmailRequested(RateLimiterType.REGISTER, dto.getEmail());
    }
    if (!res.isOK()) return res;

    res = validateRegisterDTO(dto);
    if (res.isOK()) {

      try (Handle handle = Database.getHandle()) {
        UserDao userDao = handle.attach(UserDao.class);

        User user = userDao.findByEmail(dto.getEmail());
        boolean isNotARegisteredUser = (user == null);

        if (isNotARegisteredUser) {
          String token = Tokens.add(TokenType.REGISTRATION_REQUEST, dto);

          Map<String, Object> dataMap = new HashMap<>(3);
          dataMap.put("user", dto.getEmail().split("@")[0]);
          dataMap.put("account", dto.getAccountName());
          dataMap.put("token", token);

          String message = renderer.render(EmailTemplate.REGISTRATION_REQUEST, dataMap);
          emailSender.send(
            Props.APP_EMAIL_SENDER(), 
            "About " + dto.getAccountName() + " registration on inprice.io",
            dto.getEmail(), 
            message
          );
          RedisClient.removeRequestedEmail(RateLimiterType.REGISTER, dto.getEmail());

          return Responses.OK;
        } else {
          return Responses.Already.Defined.REGISTERED_USER;
        }

      } catch (Exception e) {
        log.error("Failed to render email for activating account register", e);
        return Responses.ServerProblem.EXCEPTION;
      }
    }
    return res;
  }

  Response completeRegistration(Context ctx, String token) {
    final Response[] res = { Responses.Invalid.TOKEN };

    RegisterDTO dto = Tokens.get(TokenType.REGISTRATION_REQUEST, token);
    if (dto != null) {
      Map<String, String> clientInfo = ClientSide.getGeoInfo(ctx.req);

      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transactional -> {
          UserDao userDao = transactional.attach(UserDao.class);

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
            res[0] = 
              createAccount(
                transactional,
                user.getId(),
                user.getEmail(),
                dto.getAccountName(),
                clientInfo.get(Consts.CURRENCY_CODE),
                clientInfo.get(Consts.CURRENCY_FORMAT)
              );
          } else {
            res[0] = Responses.NotFound.USER;
          }

          if (res[0].isOK()) {
            Map<String, Object> dataMap = new HashMap<>(2);
            dataMap.put("email", dto.getEmail());
            dataMap.put("account", dto.getAccountName());
  
            String message = renderer.render(EmailTemplate.REGISTRATION_COMPLETE, dataMap);
            emailSender.send(
              Props.APP_EMAIL_SENDER(), 
              "Welcome to inprice: " + dto.getAccountName(),
              dto.getEmail(), 
              message
            );

            res[0] = new Response(user);
            Tokens.remove(TokenType.REGISTRATION_REQUEST, token);
          }

          return res[0].isOK();
        });
      }
    }

    return res[0];
  }

  Response getCurrentAccount() {
    try (Handle handle = Database.getHandle()) {
      AccountDao dao = handle.attach(AccountDao.class);
      return new Response(dao.findById(CurrentUser.getAccountId()));
    }
  }

  Response create(CreateDTO dto) {
    final Response[] res = { validateAccountDTO(dto) };

    if (res[0].isOK()) {
      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(transactional -> {
          res[0] = 
            createAccount(
              transactional,
              CurrentUser.getUserId(),
              CurrentUser.getEmail(),
              dto.getName(),
              dto.getCurrencyCode(),
              dto.getCurrencyFormat()
            );
          if (res[0].isOK()) {
            AccountDao accountDao = transactional.attach(AccountDao.class);
            Account account = accountDao.findById(res[0].getData());
            res[0] = Commons.refreshSession(account);
          }
          return res[0].isOK();
        });
      }
    }

    return res[0];
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
    final Response[] res = { Responses.DataProblem.DB_PROBLEM };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(transactional -> {
        UserDao userDao = transactional.attach(UserDao.class);
        AccountDao accountDao = transactional.attach(AccountDao.class);
        UserSessionDao sessionDao = transactional.attach(UserSessionDao.class);

        User user = userDao.findById(CurrentUser.getUserId());

        if (PasswordHelper.isValid(password, user.getPassword())) {
          Account account = accountDao.findByAdminId(CurrentUser.getUserId());

          if (account != null) {
            if (! AccountStatus.SUBSCRIBED.equals(account.getStatus())) {
              log.info("{} is being deleted. Id: {}...", account.getName(), account.getId());

              String where = "where account_id=" + CurrentUser.getAccountId();

              Batch batch = transactional.createBatch();
              batch.add("SET FOREIGN_KEY_CHECKS=0");
              batch.add("delete from import_ " + where);
              batch.add("delete from import_detail " + where);
              batch.add("delete from link_price " + where);
              batch.add("delete from link_history " + where);
              batch.add("delete from link_spec " + where);
              batch.add("delete from link " + where);
              batch.add("delete from product_tag " + where);
              batch.add("delete from product " + where);
              batch.add("delete from coupon where issued_id=" + CurrentUser.getAccountId() + " or issuer_id=" + CurrentUser.getAccountId());
              
              // in order to keep consistency, 
              // users having no account other than this must be deleted too!!!
              MemberDao memberDao = transactional.attach(MemberDao.class);
              List<Long> unboundMembers = memberDao.findUserIdListHavingJustThisAccount(CurrentUser.getAccountId());
              if (unboundMembers != null && ! unboundMembers.isEmpty()) {
                String userIdList = StringUtils.join(unboundMembers, ",");
                batch.add("delete from user where id in (" + userIdList + ")");
              }
              
              batch.add("delete from member " + where);
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
                  RedisClient.removeSesion(hash);
                }
              }

              log.info("{} is deleted. Id: {}.", account.getName(), account.getId());
              res[0] = Responses.OK;
            } else {
              res[0] = Responses.Already.ACTIVE_SUBSCRIPTION;
            }
          } else {
            res[0] = Responses.Invalid.ACCOUNT;
          }
        } else {
          res[0] = Responses.Invalid.PASSWORD;
        }

        return res[0].isOK();
      });
    }

    return res[0];
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

  private Response createAccount(Handle transactional, Long userId, String userEmail, String accountName, String currencyCode, String currencyFormat) {
    AccountDao accountDao = transactional.attach(AccountDao.class);
    MemberDao memberDao = transactional.attach(MemberDao.class);

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

        accountDao.insertStatusHistory(accountId, AccountStatus.CREATED.name());

        long memberId = 
          memberDao.insert(
            userId,
            userEmail,
            accountId,
            UserRole.ADMIN.name(),
            UserStatus.JOINED.name()
          );

        if (memberId > 0) {
          log.info("A new user just registered a new account. C.Name: {}, U.Email: {} ", accountName, userEmail);
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

    if (dto == null) {
      problem = "Invalid account info!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getName())) {
        problem = "Account name cannot be null!";
      } else if (dto.getName().length() < 3 || dto.getName().length() > 70) {
        problem = "Account name must be between 3 - 70 chars";
      }
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
    String problem = null;

    if (dto == null) {
      problem = "Invalid account info!";
    }

    if (problem == null) {
      problem = EmailValidator.verify(dto.getEmail());
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getAccountName())) {
        problem = "Account name cannot be null!";
      } else if (dto.getAccountName().length() < 3 || dto.getAccountName().length() > 70) {
        problem = "Account name must be between 3 - 70 chars";
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
