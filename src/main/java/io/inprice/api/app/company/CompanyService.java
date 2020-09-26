package io.inprice.api.app.company;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.auth.dto.PasswordDTO;
import io.inprice.api.app.company.dto.CreateDTO;
import io.inprice.api.app.company.dto.RegisterDTO;
import io.inprice.api.app.token.TokenService;
import io.inprice.api.app.token.TokenType;
import io.inprice.api.consts.Responses;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.api.external.RedisClient;
import io.inprice.api.helpers.ClientSide;
import io.inprice.api.helpers.SqlHelper;
import io.inprice.api.info.ServiceResponse;
import io.inprice.api.meta.RateLimiterType;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.utils.CurrencyFormats;
import io.inprice.api.validator.EmailValidator;
import io.inprice.api.validator.PasswordValidator;
import io.inprice.common.helpers.Beans;
import io.inprice.common.models.Company;
import io.javalin.http.Context;

class CompanyService {

  private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

  private final CompanyRepository repository = Beans.getSingleton(CompanyRepository.class);
  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);

  ServiceResponse requestRegistration(RegisterDTO dto) {
    ServiceResponse res = RedisClient.isEmailRequested(RateLimiterType.REGISTER, dto.getEmail());
    if (!res.isOK())
      return res;

    res = validateRegisterDTO(dto);
    if (res.isOK()) {

      try {
        if (! repository.hasUserDefinedTheCompany(dto)) {
          Map<String, Object> dataMap = new HashMap<>(3);
          dataMap.put("user", dto.getEmail().split("@")[0]);
          dataMap.put("company", dto.getCompanyName());
          dataMap.put("token", TokenService.add(TokenType.REGISTER_REQUEST, dto));

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

  ServiceResponse completeRegistration(Context ctx, String token) {
    ServiceResponse res = Responses.Invalid.TOKEN;

    RegisterDTO dto = TokenService.get(TokenType.REGISTER_REQUEST, token);
    if (dto != null) {
      Map<String, String> clientInfo = ClientSide.getGeoInfo(ctx.req);
      res = repository.insert(dto, clientInfo, token);
      if (res.isOK()) {
        TokenService.remove(TokenType.REGISTER_REQUEST, token);
      }
    }

    return res;
  }

  ServiceResponse getCurrentCompany() {
    Company found = repository.findById(CurrentUser.getCompanyId());
    if (found != null) {
      return new ServiceResponse(found);
    }
    return Responses.NotFound.COMPANY;
  }

  ServiceResponse create(CreateDTO dto) {
    ServiceResponse res = validateCompanyDTO(dto);
    if (res.isOK()) {
      return repository.create(dto);
    } else {
      return res;
    }
  }

  ServiceResponse update(CreateDTO dto) {
    ServiceResponse res = validateCompanyDTO(dto);
    if (res.isOK()) {
      boolean isOK = repository.update(dto);
      if (isOK) {
        return Responses.OK;
      } else {
        return Responses.DataProblem.DB_PROBLEM;
      }
    } else {
      return res;
    }
  }

  ServiceResponse deleteEverything(String password) {
    return repository.deleteEverything(password);
  }

  private ServiceResponse validateRegisterDTO(RegisterDTO dto) {
    ServiceResponse res = validateCompanyDTO(dto);

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
        return new ServiceResponse(problem);
    } else {
      return res;
    }
  }

  private ServiceResponse validateCompanyDTO(CreateDTO dto) {
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
      return new ServiceResponse(problem);
    }
  }

  private ServiceResponse validateCompanyDTO(RegisterDTO dto) {
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
      return new ServiceResponse(problem);
    }
  }

}
