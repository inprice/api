package io.inprice.scrapper.api.app.member;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.company.Company;
import io.inprice.scrapper.api.app.company.CompanyRepository;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.component.UserInfo;
import io.inprice.scrapper.api.dto.EmailValidator;
import io.inprice.scrapper.api.dto.MemberChangeStatusDTO;
import io.inprice.scrapper.api.dto.MemberDTO;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Props;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

public class MemberService {

   private static final Logger log = LoggerFactory.getLogger(MemberService.class);

   private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
   private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);
   private final TokenService tokenService = Beans.getSingleton(TokenService.class);

   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private final MemberRepository memberRepository = Beans.getSingleton(MemberRepository.class);
   private final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);

   public ServiceResponse getList() {
      return memberRepository.getList();
   }

   public ServiceResponse invite(MemberDTO memberDTO) {
      ServiceResponse res = validate(memberDTO);

      if (res.isOK()) {
         res = memberRepository.insert(memberDTO);
         if (res.isOK()) {
            res = sendMail(memberDTO);
         }
      }
      return res;
   }

   public ServiceResponse resend(long memberId) {
      ServiceResponse res = memberRepository.findById(memberId);

      if (res.isOK()) {
         Member member = res.getData();
         res = memberRepository.increaseSendingCount(memberId);
         if (res.isOK()) {
            MemberDTO memberDTO = new MemberDTO();
            memberDTO.setEmail(member.getEmail());
            memberDTO.setRole(member.getRole());
            memberDTO.setTokenType(TokenType.INVITATION_CONFIRM);
            res = sendMail(memberDTO);
         }
      }
      return res;
   }

   private ServiceResponse sendMail(MemberDTO memberDTO) {
      ServiceResponse res = companyRepository.findByAdminId(UserInfo.getCompanyId());
      if (res.isOK()) {
         Company company = res.getData();
         if (company.getId().equals(UserInfo.getCompanyId())) {
            final Map<TokenType, String> tokensMap = tokenService.getInvitationTokens(memberDTO);
            Map<String, Object> dataMap = new HashMap<>(3);
            dataMap.put("companyName", company.getName());
            dataMap.put("adminName", UserInfo.getName());
            dataMap.put("confirmToken", tokensMap.get(TokenType.INVITATION_CONFIRM));
            dataMap.put("rejectToken", tokensMap.get(TokenType.INVITATION_REJECT));
            dataMap.put("baseUrl", Props.getFrontendBaseUrl());

            String message = null;
            String templateName = null;

            ServiceResponse found = userRepository.findByEmail(memberDTO.getEmail(), false);
            if (found.isOK()) {
               templateName = "invitation-for-new-users";
               message = renderer.renderInvitationForNewUsers(dataMap);
            } else {
               templateName = "invitation-for-existing-users";
               message = renderer.renderInvitationForExistingUsers(dataMap);
            }

            if (message != null) {
               emailSender.send(Props.getEmail_Sender(),
                     "About your invitation for " + company.getName() + " at inprice.io", memberDTO.getEmail(), message);
               log.info("{} is invited as {} to {} ", memberDTO.getEmail(), memberDTO.getRole(), UserInfo.getCompanyId());
            } else {
               log.error("Template error for " + templateName + " --> " + memberDTO);
            }
         } else {
            res = new ServiceResponse("Seems that you are not the admin of this company!");
         }
      }
      return res;
   }

   public ServiceResponse changeStatus(MemberChangeStatusDTO changeStatus) {
      ServiceResponse res = memberRepository.findById(changeStatus.getMemberId());

      if (res.isOK()) {
         Member member = res.getData();

         boolean isSuitable = false;
         switch (member.getStatus()) {
            case PENDING:
               isSuitable = (changeStatus.getStatus().equals(MemberStatus.CANCELLED));
               break;
            case JOINED:
               isSuitable = (changeStatus.getStatus().equals(MemberStatus.PAUSED));
               break;
            case PAUSED:
               isSuitable = true;
               changeStatus.setPaused(true);
               break;
         }

         if (isSuitable) {
            res = memberRepository.changeStatus(changeStatus);
            if (res.isOK()) {
               log.info("{} status is changed from {} to {} ", changeStatus.getId(), member.getStatus(),
                     changeStatus.getStatus());
            }
         } else {
            res = new ServiceResponse("You cannot change " + changeStatus.getId() + " status from "
                  + member.getStatus() + " to " + changeStatus.getStatus());
         }
      }
      return res;
   }

   private ServiceResponse validate(MemberDTO memberDTO) {
      if (memberDTO == null) {
         return Responses.Invalid.INVITATION;
      }

      if (memberDTO.getRole() == null || memberDTO.getRole().equals(UserRole.ADMIN)) {
         return new ServiceResponse("Role must be either EDITOR or READER!");
      }

      String checkIfItHasAProblem = EmailValidator.verify(memberDTO.getEmail());
      if (checkIfItHasAProblem != null) {
         return new ServiceResponse(checkIfItHasAProblem);
      }

      ServiceResponse found = memberRepository.findByEmail(memberDTO.getEmail());
      if (found.isOK()) {
         return new ServiceResponse("A user with " + memberDTO.getEmail() + " address is already added to this company!");
      }

      return Responses.OK;
   }

}
