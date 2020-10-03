package io.inprice.api.app.company;

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
import io.inprice.api.app.company.dto.CreateDTO;
import io.inprice.api.app.company.dto.RegisterDTO;
import io.inprice.api.app.member.MemberDao;
import io.inprice.api.app.token.TokenType;
import io.inprice.api.app.token.Tokens;
import io.inprice.api.app.user.UserDao;
import io.inprice.api.app.user.dto.PasswordDTO;
import io.inprice.api.app.user.validator.EmailValidator;
import io.inprice.api.app.user.validator.PasswordValidator;
import io.inprice.api.consts.Consts;
import io.inprice.api.consts.Responses;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.Response;
import io.inprice.api.meta.RateLimiterType;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.CurrencyFormats;
import io.inprice.common.helpers.Beans;
import io.inprice.common.helpers.Database;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Company;
import io.inprice.common.models.User;
import io.javalin.http.Context;
import jodd.util.BCrypt;

class CompanyService {

  private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);

  Response requestRegistration(RegisterDTO dto) {
    Response res = RedisClient.isEmailRequested(RateLimiterType.REGISTER, dto.getEmail());
    if (!res.isOK())
      return res;

    res = validateRegisterDTO(dto);
    if (res.isOK()) {

      try (Handle handle = Database.getHandle()) {
        UserDao userDao = handle.attach(UserDao.class);
        CompanyDao companyDao = handle.attach(CompanyDao.class);

        boolean isCompanyDefined = false;

        User user = userDao.findByEmail(dto.getEmail());
        if (user != null) {
          Company found = companyDao.findByNameAndAdminId(dto.getCompanyName(), user.getId());
          isCompanyDefined = (found != null);
        }

        if (! isCompanyDefined) {
          Map<String, Object> dataMap = new HashMap<>(3);
          dataMap.put("user", dto.getEmail().split("@")[0]);
          dataMap.put("company", dto.getCompanyName());
          dataMap.put("token", Tokens.add(TokenType.REGISTER_REQUEST, dto));

          String message = renderer.renderRegisterActivationLink(dataMap);
          emailSender.send(
            Props.APP_EMAIL_SENDER(), 
            "About " + dto.getCompanyName() + " registration on inprice.io",
            dto.getEmail(), 
            message
          );
          RedisClient.removeRequestedEmail(RateLimiterType.REGISTER, dto.getEmail());

          return Responses.OK;
        } else {
          return Responses.Already.Defined.COMPANY;
        }

      } catch (Exception e) {
        log.error("An error occurred in rendering email for activating register company", e);
        return Responses.ServerProblem.EXCEPTION;
      }
    }
    return res;
  }

  Response completeRegistration(Context ctx, String token) {
    final Response[] res = { Responses.Invalid.TOKEN };

    RegisterDTO dto = Tokens.get(TokenType.REGISTER_REQUEST, token);
    if (dto != null) {
      Map<String, String> clientInfo = ClientSide.getGeoInfo(ctx.req);

      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(h -> {
          UserDao userDao = handle.attach(UserDao.class);

          User user = userDao.findByEmail(dto.getEmail());
          if (user == null) {
            user = new User();
            user.setEmail(dto.getEmail());
            user.setName(dto.getEmail().split("@")[0]);
            user.setTimezone(clientInfo.get(Consts.TIMEZONE));
            user.setCreatedAt(new Date());
  
            final String salt = BCrypt.gensalt(Props.APP_SALT_ROUNDS());
            final String hash = BCrypt.hashpw(dto.getPassword(), salt);
  
            user.setId(
              userDao.insert(
                user.getEmail(), 
                user.getName(), 
                clientInfo.get(Consts.TIMEZONE), 
                salt, hash
              )
            );
          }
    
          if (user.getId() != null) {
            res[0] = 
              createCompany(
                handle,
                user.getId(),
                user.getEmail(),
                dto.getCompanyName(),
                clientInfo.get(Consts.CURRENCY_CODE),
                clientInfo.get(Consts.CURRENCY_FORMAT)
              );
          } else {
            res[0] = Responses.NotFound.USER;
          }

          if (res[0].isOK()) {
            Tokens.remove(TokenType.REGISTER_REQUEST, token);
          }

          return res[0].isOK();
        });
      }
    }

    return res[0];
  }

  Response getCurrentCompany() {
    try (Handle handle = Database.getHandle()) {
      CompanyDao dao = handle.attach(CompanyDao.class);
      return new Response(dao.findById(CurrentUser.getCompanyId()));
    }
  }

  Response create(CreateDTO dto) {
    final Response[] res = { validateCompanyDTO(dto) };

    if (res[0].isOK()) {
      try (Handle handle = Database.getHandle()) {
        handle.inTransaction(h -> {
          res[0] = 
            createCompany(
              handle,
              CurrentUser.getUserId(),
              CurrentUser.getEmail(),
              dto.getName(),
              dto.getCurrencyCode(),
              dto.getCurrencyFormat()
            );
          return true;
        });
      }
    }

    return res[0];
  }

  Response update(CreateDTO dto) {
    Response res = validateCompanyDTO(dto);

    if (res.isOK()) {
      boolean isOK = false;

      try (Handle handle = Database.getHandle()) {
        CompanyDao companyDao = handle.attach(CompanyDao.class);
        isOK =
          companyDao.update(
            dto.getName(),
            dto.getCurrencyCode(),
            dto.getCurrencyFormat(),
            CurrentUser.getCompanyId(),
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

  Response deleteEverything(String password) {
    final Response[] res = { Responses.DataProblem.DB_PROBLEM };

    try (Handle handle = Database.getHandle()) {
      handle.inTransaction(h -> {
        UserDao userDao = h.attach(UserDao.class);
        CompanyDao companyDao = h.attach(CompanyDao.class);
        UserSessionDao sessionDao = h.attach(UserSessionDao.class);

        User user = userDao.findById(CurrentUser.getUserId());
        String phash = BCrypt.hashpw(password, user.getPasswordSalt());

        if (phash.equals(user.getPasswordHash())) {
          Company company = companyDao.findByAdminId(CurrentUser.getCompanyId());

          if (company != null) {
            String where = "where company_id=" + CurrentUser.getCompanyId();

            Batch batch = h.createBatch();
            batch.add("SET FOREIGN_KEY_CHECKS=0");
            batch.add("delete from competitor_price " + where);
            batch.add("delete from competitor_history " + where);
            batch.add("delete from competitor_spec " + where);
            batch.add("delete from competitor " + where);
            batch.add("delete from product_price " + where);
            batch.add("delete from product " + where);
            batch.add("delete from lookup " + where);
            batch.add("delete from user_session " + where);
            batch.add("delete from member " + where);
            batch.add("delete from subs_trans " + where);
            batch.add("delete from user where id in (select admin_id from company where id="+CurrentUser.getCompanyId()+")");
            batch.add("delete from company where id="+CurrentUser.getCompanyId());
            batch.add("SET FOREIGN_KEY_CHECKS=1");
            batch.execute();

            List<String> hashList = sessionDao.findHashesByCompanyId(CurrentUser.getCompanyId());
            if (hashList != null && hashList.size() > 0) {
              for (String hash : hashList) {
                RedisClient.removeSesion(hash);
              }
            }
            res[0] = Responses.OK;
          } else {
            res[0] = Responses.Invalid.COMPANY;
          }
        } else {
          res[0] = Responses.Invalid.USER;
        }

        return res[0].isOK();
      });
    }

    return res[0];
  }

  private Response validateRegisterDTO(RegisterDTO dto) {
    Response res = validateCompanyDTO(dto);

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

  private Response createCompany(Handle handle, Long userId, String userEmail, String companyName, String currencyCode, String currencyFormat) {
    CompanyDao companyDao = handle.attach(CompanyDao.class);
    MemberDao memberDao = handle.attach(MemberDao.class);

    Company company = companyDao.findByNameAndAdminId(companyName, userId);
    if (company == null) {
      Long companyId = 
        companyDao.insert(
          userId, 
          companyName,
          currencyCode,
          currencyFormat
        );

      if (companyId != null) {
        long memberId = 
          memberDao.insert(
            userId,
            userEmail,
            companyId,
            UserRole.ADMIN.name(),
            UserStatus.JOINED.name()
          );

        if (memberId > 0) {
          log.info("A new user just registered a new company. C.Name: {}, U.Email: {} ", companyName, userEmail);
          return Responses.OK;
        }
      }
      return Responses.DataProblem.DB_PROBLEM;
    } else {
      return Responses.Already.Defined.COMPANY;
    }
  }

  private Response validateCompanyDTO(CreateDTO dto) {
    String problem = null;

    if (dto == null) {
      problem = "Invalid company info!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getName())) {
        problem = "Company name cannot be null!";
      } else if (dto.getName().length() < 3 || dto.getName().length() > 70) {
        problem = "Company name must be between 3 - 70 chars";
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

  private Response validateCompanyDTO(RegisterDTO dto) {
    String problem = null;

    if (dto == null) {
      problem = "Invalid company info!";
    }

    if (problem == null) {
      problem = EmailValidator.verify(dto.getEmail());
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getCompanyName())) {
        problem = "Company name cannot be null!";
      } else if (dto.getCompanyName().length() < 3 || dto.getCompanyName().length() > 70) {
        problem = "Company name must be between 3 - 70 chars";
      }
    }

    if (problem == null) {
      //cleaning
      dto.setEmail(SqlHelper.clear(dto.getEmail()));
      dto.setCompanyName(SqlHelper.clear(dto.getCompanyName()));
      dto.setPassword(SqlHelper.clear(dto.getPassword()));
      dto.setRepeatPassword(SqlHelper.clear(dto.getRepeatPassword()));

      return Responses.OK;
    } else {
      return new Response(problem);
    }
  }

}
