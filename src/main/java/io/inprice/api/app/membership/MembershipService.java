package io.inprice.api.app.membership;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.app.auth.dto.InvitationAcceptDTO;
import io.inprice.api.app.auth.dto.InvitationSendDTO;
import io.inprice.api.app.auth.dto.InvitationUpdateDTO;
import io.inprice.api.app.auth.dto.PasswordDTO;
import io.inprice.api.app.token.TokenType;
import io.inprice.api.app.token.Tokens;
import io.inprice.api.app.user.UserRepository;
import io.inprice.api.consts.Responses;
import io.inprice.api.email.EmailSender;
import io.inprice.api.email.TemplateRenderer;
import io.inprice.api.external.Props;
import io.inprice.api.info.Response;
import io.inprice.api.session.CurrentUser;
import io.inprice.api.validator.EmailValidator;
import io.inprice.api.validator.PasswordValidator;
import io.inprice.common.helpers.Beans;
import io.inprice.common.meta.UserRole;
import io.inprice.common.meta.UserStatus;
import io.inprice.common.models.Membership;
import io.inprice.common.models.User;

public class MembershipService {

  private static final Logger log = LoggerFactory.getLogger(MembershipService.class);

  private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
  private final MembershipRepository membershipRepository = Beans.getSingleton(MembershipRepository.class);

  private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
  private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);

  public Response getList() {
    return membershipRepository.getList();
  }

  public Response invite(InvitationSendDTO dto) {
    Response res = validate(dto);
    if (res.isOK()) {

      res = membershipRepository.findByEmailAndCompanyId(dto.getEmail(), CurrentUser.getCompanyId());
      if (!res.isOK()) {
        dto.setCompanyId(CurrentUser.getCompanyId());
        res = membershipRepository.invite(dto);
        if (res.isOK()) {
          res = sendMail(dto);
        }
      } else {
        return new Response("A user with " + dto.getEmail() + " address is already added to this company!");
      }
    }
    return res;
  }

  public Response resend(long memId) {
    Response res = membershipRepository.findById(memId);

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

  public Response delete(long memId) {
    Response res = membershipRepository.findById(memId);

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

  public Response changeRole(InvitationUpdateDTO dto) {
    String problem = validate(dto);

    if (problem == null) {
      return membershipRepository.changeRole(dto);
    }
    return new Response(problem);
  }

  public Response pause(Long id) {
    return membershipRepository.changeStatus(id, false);
  }

  public Response resume(Long id) {
    return membershipRepository.changeStatus(id, true);
  }

  public Response acceptNewUser(InvitationAcceptDTO dto, String timezone) {
    String problem = validate(dto);

    if (problem == null) {
      InvitationSendDTO invitationDTO = Tokens.get(TokenType.INVITATION, dto.getToken());
      if (invitationDTO != null) {
        Response res = membershipRepository.acceptNewUser(dto, timezone, invitationDTO);
        if (res.isOK()) {
          Tokens.remove(TokenType.INVITATION, dto.getToken());
          return res;
        }
      } else {
        return Responses.Invalid.TOKEN;
      }
    }

    return new Response(problem);
  }

  private Response sendMail(InvitationSendDTO dto) {
    Map<String, Object> dataMap = new HashMap<>(5);
    dataMap.put("company", CurrentUser.getCompanyName());
    dataMap.put("admin", CurrentUser.getUserName());

    String message = null;
    String templateName = null;

    Response found = userRepository.findByEmail(dto.getEmail(), false);
    if (found.isOK()) {
      User user = found.getData();
      dataMap.put("user", user.getName());

      templateName = "invitation-for-existing-users";
      message = renderer.renderInvitationForExistingUsers(dataMap);
    } else {
      dataMap.put("user", dto.getEmail().substring(0, dto.getEmail().indexOf('@')));
      dataMap.put("token", Tokens.add(TokenType.INVITATION, dto));
      dataMap.put("url", Props.APP_WEB_URL() + "/accept-invitation");

      templateName = "invitation-for-new-users";
      message = renderer.renderInvitationForNewUsers(dataMap);
    }

    if (message != null) {
      emailSender.send(Props.APP_EMAIL_SENDER(),
          "About your invitation for " + CurrentUser.getCompanyName() + " at inprice.io", dto.getEmail(), message);

      log.info("{} is invited as {} to {} ", dto.getEmail(), dto.getRole(), CurrentUser.getCompanyId());
      log.info(message);
      return Responses.OK;
    } else {
      log.error("Template error for " + templateName + " --> " + dto);
      return Responses.ServerProblem.FAILED;
    }
  }

  private Response validate(InvitationSendDTO dto) {
    if (dto == null
        || (dto.getEmail() != null && dto.getEmail().toLowerCase().equals(CurrentUser.getEmail().toLowerCase()))) {
      return Responses.Invalid.INVITATION;
    }

    if (dto.getRole() == null || dto.getRole().equals(UserRole.ADMIN)) {
      return new Response(
          String.format("Role must be either %s or %s!", UserRole.EDITOR.name(), UserRole.VIEWER.name()));
    }

    String checkIfItHasAProblem = EmailValidator.verify(dto.getEmail());
    if (checkIfItHasAProblem != null) {
      return new Response(checkIfItHasAProblem);
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
