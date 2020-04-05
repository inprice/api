package io.inprice.scrapper.api.app.invitation;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.company.Company;
import io.inprice.scrapper.api.app.company.CompanyRepository;
import io.inprice.scrapper.api.app.member.Member;
import io.inprice.scrapper.api.app.member.MemberRole;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.User;
import io.inprice.scrapper.api.app.user.UserRepository;
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
   private final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);

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

   public ServiceResponse resend(long memberId) {
      ServiceResponse res = invitationRepository.findById(memberId);

      if (res.isOK()) {
         Member member = res.getData();
         res = invitationRepository.increaseSendingCount(memberId);
         if (res.isOK()) {
            InvitationDTO dto = new InvitationDTO();
            dto.setCompanyId(CurrentUser.getCompanyId());
            dto.setEmail(member.getEmail());
            dto.setRole(member.getRole());
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

   public ServiceResponse acceptExisting(Long memberId) {
      return invitationRepository.handleExisting(memberId, true);
   }

   public ServiceResponse rejectExisting(Long memberId) {
      return invitationRepository.handleExisting(memberId, false);
   }

   private ServiceResponse sendMail(InvitationDTO dto) {
      ServiceResponse res = companyRepository.findByAdminId(CurrentUser.getCompanyId());
      if (res.isOK()) {
         Company company = res.getData();
         if (company.getId().equals(CurrentUser.getCompanyId())) {
            Map<String, Object> dataMap = new HashMap<>(5);
            dataMap.put("company", company.getName());
            dataMap.put("admin", CurrentUser.getName());

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
                     "About your invitation for " + company.getName() + " at inprice.io", dto.getEmail(), message);

               res = Responses.OK;
               log.info("{} is invited as {} to {} ", dto.getEmail(), dto.getRole(), CurrentUser.getCompanyId());
            } else {
               res = Responses.ServerProblem.FAILED;
               log.error("Template error for " + templateName + " --> " + dto);
            }
         } else {
            res = new ServiceResponse("Seems that you are not the admin of this company!");
         }
      }
      return res;
   }

   private ServiceResponse validate(InvitationDTO dto) {
      if (dto == null) {
         return Responses.Invalid.INVITATION;
      }

      if (dto.getRole() == null || dto.getRole().equals(MemberRole.ADMIN)) {
         return new ServiceResponse(String.format("Role must be either %s or %s!", MemberRole.EDITOR.name(), MemberRole.READER.name()));
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

         if (! TokenService.isTokenValid(dto.getToken())) {
            problem = Responses.Invalid.TOKEN.getReason();
         }
      }

      if (problem == null)
         return Responses.OK;
      else
         return new ServiceResponse(problem);
   }

}
