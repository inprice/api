package io.inprice.scrapper.api.app.member;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.component.UserInfo;
import io.inprice.scrapper.api.dto.EmailValidator;
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
