package io.inprice.scrapper.api.app.company;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.auth.AuthService;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.CreateCompanyDTO;
import io.inprice.scrapper.api.dto.EmailValidator;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.dto.RegisterDTO;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.external.Database;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.ClientSide;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.utils.CurrencyFormats;
import io.javalin.http.Context;

public class CompanyService {

  private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

  private final AuthService authService = Beans.getSingleton(AuthService.class);
  private final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);
  private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
  private final Database db = Beans.getSingleton(Database.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);

  public ServiceResponse requestRegistration(RegisterDTO dto, String ip) {
    ServiceResponse res = RedisClient.isIpRateLimited(RateLimiterType.REGISTER, ip);
    if (!res.isOK())
      return res;

    res = validateRegisterDTO(dto);
    if (res.isOK()) {

      try (Connection con = db.getConnection()) {
        ServiceResponse found = userRepository.findByEmail(con, dto.getEmail());
        if (found.isOK()) {
          // checks if the user has already defined the same company
          User user = found.getData();
          found = companyRepository.findByNameAndAdminId(con, dto.getCompanyName(), user.getId());
          if (found.isOK()) {
            return Responses.Already.Defined.COMPANY;
          }
        }

        Map<String, Object> dataMap = new HashMap<>(3);
        dataMap.put("user", dto.getEmail().split("@")[0]);
        dataMap.put("company", dto.getCompanyName());
        dataMap.put("token", TokenService.add(TokenType.REGISTER_REQUEST, dto));

        final String message = renderer.renderRegisterActivationLink(dataMap);
        emailSender.send(Props.getEmail_Sender(), "About " + dto.getCompanyName() + " registration on inprice.io",
            dto.getEmail(), message);

        return Responses.OK;
      } catch (Exception e) {
        log.error("An error occurred in rendering email for activating register company", e);
        return Responses.ServerProblem.EXCEPTION;
      }
    }
    return res;
  }

  public ServiceResponse completeRegistration(Context ctx, String token) {
    RegisterDTO dto = TokenService.get(TokenType.REGISTER_REQUEST, token);
    if (dto != null) {
      Map<String, String> clientInfo = ClientSide.getGeoInfo(ctx.req);
      ServiceResponse res = companyRepository.insert(dto, clientInfo, token);
      if (res.isOK()) {
        return authService.createSession(ctx, res.getData());
      }
      return res;
    } else if (StringUtils.isNotBlank(token)) {
      TokenService.get(TokenType.REGISTER_REQUEST, token);
    }
    return Responses.Invalid.TOKEN;
  }

  public ServiceResponse getCurrentCompany() {
    return companyRepository.findById(CurrentUser.getCompanyId());
  }

  public ServiceResponse create(CreateCompanyDTO dto) {
    ServiceResponse res = validateCompanyDTO(dto);
    if (res.isOK()) {
      return companyRepository.create(dto);
    } else {
      return res;
    }
  }

  public ServiceResponse update(CreateCompanyDTO dto) {
    ServiceResponse res = validateCompanyDTO(dto);
    if (res.isOK()) {
      return companyRepository.update(dto);
    } else {
      return res;
    }
  }

  public ServiceResponse findById(Long id) {
    return companyRepository.findById(id);
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

  private ServiceResponse validateCompanyDTO(CreateCompanyDTO dto) {
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
      } else if (dto.getName().length() < 3 || dto.getName().length() > 16) {
        problem = "Currency format must be between 3 - 16 chars";
      }
    }

    if (problem == null)
      return Responses.OK;
    else
      return new ServiceResponse(problem);
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

    if (problem == null)
      return Responses.OK;
    else
      return new ServiceResponse(problem);
  }

}
