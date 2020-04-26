package io.inprice.scrapper.api.app.invitation;

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
import io.inprice.scrapper.api.app.user_company.UserCompany;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.EmailValidator;
import io.inprice.scrapper.api.dto.InvitationAcceptDTO;
import io.inprice.scrapper.api.dto.InvitationDTO;
import io.inprice.scrapper.api.dto.NameAndEmailValidator;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class InvitationService {

   private static final Logger log = LoggerFactory.getLogger(InvitationService.class);

   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private final InvitationRepository invitationRepository = Beans.getSingleton(InvitationRepository.class);

   private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
   private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);

   public ServiceResponse send(InvitationDTO dto) {
      ServiceResponse res = validate(dto);

      if (res.isOK()) {
         dto.setCompanyId(CurrentUser.getCompanyId());
         res = invitationRepository.invite(dto);
         if (res.isOK()) {
            res = sendMail(dto);
         }
      }
      return res;
   }

   public ServiceResponse resend(long invitationId) {
      ServiceResponse res = invitationRepository.findById(invitationId);

      if (res.isOK()) {
         UserCompany userCoompany = res.getData();
         res = invitationRepository.increaseSendingCount(invitationId);
         if (res.isOK()) {
            InvitationDTO dto = new InvitationDTO();
            dto.setCompanyId(CurrentUser.getCompanyId());
            dto.setEmail(userCoompany.getEmail());
            dto.setRole(userCoompany.getRole());
            res = sendMail(dto);
         }
      }
      return res;
   }

   public ServiceResponse acceptNewUser(InvitationAcceptDTO acceptDTO) {
      ServiceResponse res = validate(acceptDTO);

      if (res.isOK()) {
         InvitationDTO invitationDTO = TokenService.get(TokenType.INVITATION, acceptDTO.getToken());
         if (invitationDTO != null) {
            res = invitationRepository.acceptNewUser(acceptDTO, invitationDTO);
            if (res.isOK()) {
               TokenService.remove(TokenType.INVITATION, acceptDTO.getToken());
            }
         } else {
            return Responses.Invalid.TOKEN;
         }
      }
      return res;
   }

   public ServiceResponse acceptExisting(Long invitationId) {
      return invitationRepository.handleExisting(invitationId, true);
   }

   public ServiceResponse rejectExisting(Long invitationId) {
      return invitationRepository.handleExisting(invitationId, false);
   }

   private ServiceResponse sendMail(InvitationDTO dto) {
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
         dataMap.put("user", dto.getEmail().substring(0, dto.getEmail().indexOf('@')-1));
         dataMap.put("token", TokenService.add(TokenType.INVITATION, dto));
         dataMap.put("url", Props.getWebUrl() + "/accept-invitation");

         templateName = "invitation-for-new-users";
         message = renderer.renderInvitationForNewUsers(dataMap);
      }

      if (message != null) {
         emailSender.send(
            Props.getEmail_Sender(),
               "About your invitation for " + CurrentUser.getCompanyName() + " at inprice.io", dto.getEmail(), message);

         log.info("{} is invited as {} to {} ", dto.getEmail(), dto.getRole(), CurrentUser.getCompanyId());
         return Responses.OK;
      } else {
         log.error("Template error for " + templateName + " --> " + dto);
         return Responses.ServerProblem.FAILED;
      }
   }

   private ServiceResponse validate(InvitationDTO dto) {
      if (dto == null) {
         return Responses.Invalid.INVITATION;
      }

      if (CurrentUser.getRole().equals(UserRole.ADMIN)) {
         return new ServiceResponse("Only admins can send an invitation!");
      }

      if (dto.getRole() == null || dto.getRole().equals(UserRole.ADMIN)) {
         return new ServiceResponse(String.format("Role must be either %s or %s!", UserRole.EDITOR.name(), UserRole.VIEWER.name()));
      }

      String checkIfItHasAProblem = EmailValidator.verify(dto.getEmail());
      if (checkIfItHasAProblem != null) {
         return new ServiceResponse(checkIfItHasAProblem);
      }

      ServiceResponse found = invitationRepository.findByEmailAndCompanyId(dto.getEmail(), CurrentUser.getCompanyId());
      if (found.isOK()) {
         return new ServiceResponse("A user with " + dto.getEmail() + " address is already added to this company!");
      }

      return Responses.OK;
   }

   private ServiceResponse validate(InvitationAcceptDTO dto) {
      String problem = NameAndEmailValidator.verifyName(dto.getName());

      if (problem == null) {
         PasswordDTO pswDTO = new PasswordDTO();
         pswDTO.setPassword(dto.getPassword());
         pswDTO.setRepeatPassword(dto.getRepeatPassword());
         problem = PasswordValidator.verify(pswDTO, true, false);
      }

      if (problem == null) {
         if (StringUtils.isBlank(dto.getToken())) {
            problem = Responses.Invalid.TOKEN.getReason();
         }
      }

      if (problem == null)
         return Responses.OK;
      else
         return new ServiceResponse(problem);
   }

}
