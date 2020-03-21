package io.inprice.scrapper.api.app.member;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.company.Company;
import io.inprice.scrapper.api.app.company.CompanyRepository;
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

   private static final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
   private static final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);

   private static final MemberRepository memberRepository = Beans.getSingleton(MemberRepository.class);
   private static final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);

   public ServiceResponse invite(MemberDTO memberDTO) {
      ServiceResponse res = validate(memberDTO);

      if (res.isOK()) {
         res = companyRepository.findByAdminId(UserInfo.getCompanyId());
         if (res.isOK()) {
            Company company = res.getData();
            res = memberRepository.insert(memberDTO);
            if (res.isOK()) {
               Map<String, Object> dataMap = new HashMap<>(3);
               dataMap.put("companyName", company.getName());
               dataMap.put("adminName", UserInfo.getName());
               dataMap.put("confirmToken", confirmToken);
               dataMap.put("rejectToken", rejectToken);
               dataMap.put("baseUrl", Props.getFrontendBaseUrl());

               final String message = renderer.renderInvitationForNewUsers(dataMap);
               emailSender.send(Props.getEmail_Sender(), "About your invitation", memberDTO.getEmail(), message);
               log.info("{} is invited as {} to {} ", memberDTO.getEmail(), memberDTO.getRole(), UserInfo.getCompanyId());
            }
         }
      }
      return res;
   }

   public ServiceResponse changeStatus(MemberChangeStatusDTO changeStatus) {
      ServiceResponse res = memberRepository.findByEmail(changeStatus.getEmail());

      if (res.isOK()) {
         Member member = res.getData();

         boolean isSuitable = false;
         if (MemberStatus.PENDING.equals(member.getStatus())) {
            isSuitable = (MemberStatus.CANCELLED.equals(changeStatus.getStatus()));
         } else if (MemberStatus.JOINED.equals(member.getStatus())) {
            isSuitable = (MemberStatus.PAUSED.equals(changeStatus.getStatus()));
         }

         if (isSuitable) {
            res = memberRepository.changeStatus(changeStatus);
            if (res.isOK()) {
               log.info("{} status is changed from {} to {} ", changeStatus.getEmail(), member.getStatus(),
                     changeStatus.getStatus());
            }
         } else {
            res = new ServiceResponse("You cannot change " + changeStatus.getEmail() + " status from "
                  + member.getStatus() + " to " + changeStatus.getStatus());
         }
      }
      return res;
   }

   private ServiceResponse validate(MemberDTO memberDTO) {
      if (memberDTO == null) {
         return Responses.Invalid.INVITATION;
      }

      if (UserInfo.getRole().equals(UserRole.ADMIN)) {
         return Responses.PermissionProblem.ADMIN_ONLY;
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
         return new ServiceResponse(
               "A user with " + memberDTO.getEmail() + " address is already added to this company!");
      }

      return Responses.OK;
   }

}
