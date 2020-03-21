package io.inprice.scrapper.api.app.member;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.app.user.UserStatus;
import io.inprice.scrapper.api.component.UserInfo;
import io.inprice.scrapper.api.dto.EmailValidator;
import io.inprice.scrapper.api.dto.MemberChangeStatusDTO;
import io.inprice.scrapper.api.dto.MemberDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

public class MemberService {

   private static final Logger log = LoggerFactory.getLogger(MemberService.class);

   private static final MemberRepository repository = Beans.getSingleton(MemberRepository.class);

   public ServiceResponse invite(MemberDTO memberDTO) {
      ServiceResponse res = validate(memberDTO);

      if (res.isOK()) {
         res = repository.insert(memberDTO);
         if (res.isOK()) {
            log.info("{} is invited as {} to {} ", memberDTO.getEmail(), memberDTO.getRole(), UserInfo.getCompanyId());
         }
      }
      return res;
   }

   public ServiceResponse changeStatus(MemberChangeStatusDTO changeStatus) {
      ServiceResponse res = repository.findByEmail(changeStatus.getEmail());

      if (res.isOK()) {
         Member member = res.getData();

         boolean isSuitable = false;
         if (UserStatus.PENDING.equals(member.getStatus())) {
            isSuitable = (UserStatus.CANCELLED.equals(changeStatus.getStatus()));
         } else if (UserStatus.JOINED.equals(member.getStatus())) {
            isSuitable = (UserStatus.PUASED.equals(changeStatus.getStatus()));
         }

         if (isSuitable) {
            res = repository.changeStatus(changeStatus);
            if (res.isOK()) {
               log.info("{} status is changed from {} to {} ", changeStatus.getEmail(), member.getStatus(), changeStatus.getStatus());
            }
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

      ServiceResponse found = repository.findByEmail(memberDTO.getEmail());
      if (found.isOK()) {
         return new ServiceResponse(
               "A user with " + memberDTO.getEmail() + " address is already added to this company!");
      }

      return Responses.OK;
   }

}
