package io.inprice.scrapper.api.app.membership;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.EmailValidator;
import io.inprice.scrapper.api.dto.InvitationAcceptDTO;
import io.inprice.scrapper.api.dto.InvitationSendDTO;
import io.inprice.scrapper.api.dto.InvitationUpdateDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class MembershipService {

  private static final Logger log = LoggerFactory.getLogger(MembershipService.class);

  private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
  private final MembershipRepository membershipRepository = Beans.getSingleton(MembershipRepository.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);

  public ServiceResponse getList() {
    return membershipRepository.getList();
  }

  public ServiceResponse invite(InvitationSendDTO dto) {
    ServiceResponse res = validate(dto);
    if (res.isOK()) {

      res = membershipRepository.findByEmailAndCompanyId(dto.getEmail(), CurrentUser.getCompanyId());
      if (!res.isOK()) {
        dto.setCompanyId(CurrentUser.getCompanyId());
        res = membershipRepository.invite(dto);
        if (res.isOK()) {
          res = sendMail(dto);
        }
      } else {
        return new ServiceResponse("A user with " + dto.getEmail() + " address is already added to this company!");
      }
    }
    return res;
  }

  public ServiceResponse resend(long memId) {
    ServiceResponse res = membershipRepository.findById(memId);

    if (res.isOK()) {
      Membership membership = res.getData();
      res = membershipRepository.increaseSendingCount(memId);
      if (res.isOK()) {
        InvitationSendDTO dto = new InvitationSendDTO();
        dto.setCompanyId(CurrentUser.getCompanyId());
        dto.setEmail(membership.getEmail());
        dto.setRole(membership.getRole());
        res = sendMail(dto);
      }
    }
    return res;
  }

  public ServiceResponse delete(long memId) {
    ServiceResponse res = membershipRepository.findById(memId);

    if (res.isOK()) {
      Membership membership = res.getData();
      if (!membership.getStatus().equals(UserStatus.DELETED)) {
        res = membershipRepository.delete(memId);
      } else {
        res = Responses.Already.DELETED_MEMBER;
      }
    }
    return res;
  }

  public ServiceResponse changeRole(InvitationUpdateDTO dto) {
    String problem = validate(dto);

    if (problem == null) {
      return membershipRepository.changeRole(dto);
    }
    return new ServiceResponse(problem);
  }

  public ServiceResponse pause(Long id) {
    return membershipRepository.changeStatus(id, false);
  }

  public ServiceResponse resume(Long id) {
    return membershipRepository.changeStatus(id, true);
  }

  public ServiceResponse acceptNewUser(InvitationAcceptDTO dto, String timezone) {
    String problem = validate(dto);

    if (problem == null) {
      InvitationSendDTO invitationDTO = TokenService.get(TokenType.INVITATION, dto.getToken());
      if (invitationDTO != null) {
        ServiceResponse res = membershipRepository.acceptNewUser(dto, timezone, invitationDTO);
        if (res.isOK()) {
          TokenService.remove(TokenType.INVITATION, dto.getToken());
        }
      } else {
        return Responses.Invalid.TOKEN;
      }
    }

    return new ServiceResponse(problem);
  }

  private ServiceResponse sendMail(InvitationSendDTO dto) {
    Map<String, Object> dataMap = new HashMap<>(5);
    dataMap.put("company", CurrentUser.getCompanyName());
    dataMap.put("admin", CurrentUser.getUserName());

    String message = null;
    String templateName = null;

    ServiceResponse found = userRepository.findByEmail(dto.getEmail(), false);
    if (found.isOK()) {
      User user = found.getData();
      dataMap.put("user", user.getName());

      templateName = "invitation-for-existing-users";
      message = renderer.renderInvitationForExistingUsers(dataMap);
    } else {
      dataMap.put("user", dto.getEmail().substring(0, dto.getEmail().indexOf('@') - 1));
      dataMap.put("token", TokenService.add(TokenType.INVITATION, dto));
      dataMap.put("url", Props.getWebUrl() + "/accept-invitation");

      templateName = "invitation-for-new-users";
      message = renderer.renderInvitationForNewUsers(dataMap);
    }

    if (message != null) {
      emailSender.send(Props.getEmail_Sender(),
          "About your invitation for " + CurrentUser.getCompanyName() + " at inprice.io", dto.getEmail(), message);

      log.info("{} is invited as {} to {} ", dto.getEmail(), dto.getRole(), CurrentUser.getCompanyId());
      return Responses.OK;
    } else {
      log.error("Template error for " + templateName + " --> " + dto);
      return Responses.ServerProblem.FAILED;
    }
  }

  private ServiceResponse validate(InvitationSendDTO dto) {
    if (dto == null
        || (dto.getEmail() != null && dto.getEmail().toLowerCase().equals(CurrentUser.getEmail().toLowerCase()))) {
      return Responses.Invalid.INVITATION;
    }

    if (dto.getRole() == null || dto.getRole().equals(UserRole.ADMIN)) {
      return new ServiceResponse(
          String.format("Role must be either %s or %s!", UserRole.EDITOR.name(), UserRole.VIEWER.name()));
    }

    String checkIfItHasAProblem = EmailValidator.verify(dto.getEmail());
    if (checkIfItHasAProblem != null) {
      return new ServiceResponse(checkIfItHasAProblem);
    }

    return Responses.OK;
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

  private String validate(InvitationAcceptDTO dto) {
    String problem = null;

    if (dto == null) {
      problem = "Invalid invitation data!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getToken())) {
        problem = Responses.Invalid.TOKEN.getReason();
      }
    }

    if (problem == null) {
      PasswordDTO pswDTO = new PasswordDTO();
      pswDTO.setPassword(dto.getPassword());
      pswDTO.setRepeatPassword(dto.getRepeatPassword());
      problem = PasswordValidator.verify(pswDTO, true, false);
    }

    return problem;
  }

}
