package io.inprice.scrapper.api.app.member;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.auth.AuthService;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.MemberChangeRoleDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class MemberService {

   private static final Logger log = LoggerFactory.getLogger(MemberService.class);

   private final AuthService authService = Beans.getSingleton(AuthService.class);
   private final MemberRepository memberRepository = Beans.getSingleton(MemberRepository.class);

   public ServiceResponse getList() {
      return memberRepository.getListByCompany();
   }

   public ServiceResponse deleteById(Long id) {
      if (id != null && id > 0) {
         Member member = memberRepository.getById(id);
         if (member != null) {
            ServiceResponse res = memberRepository.deleteById(member.getId());
            if (res.isOK()) {
               authService.closeSession(member.getEmail());
               return Responses.OK;
            }
         }
      }
      return Responses.NotFound.MEMBER;
   }

   public ServiceResponse toggleStatus(Long id) {
      if (id != null && id > 0) {
         Member member = memberRepository.getById(id);
         if (member != null) {
            ServiceResponse res = memberRepository.toggleStatus(id);
            if (res.isOK()) {
               if (member.getActive().equals(Boolean.TRUE)) {
                  authService.closeSession(member.getEmail());
               }
               return Responses.OK;
            }
         }
      }
      return Responses.NotFound.MEMBER;
   }

   public ServiceResponse changeRole(MemberChangeRoleDTO dto) {
      ServiceResponse res = validate(dto);
      if (res.isOK()) {

         Member member = res.getData();
         if (! member.getRole().equals(MemberRole.ADMIN)) {
            res = memberRepository.changeRole(dto);
            if (res.isOK()) {
               log.info("{} role is changed to {} ", dto.getMemberId(), member.getRole());
            }
         } else {
            res = new ServiceResponse("Admin's role cannot be changed!");
         }
      }
      return res;
   }

   private ServiceResponse validate(MemberChangeRoleDTO dto) {
      if (dto == null || dto.getMemberId() == null) {
         return new ServiceResponse("Member Id field cannot be empty!");
      }

      if (! CurrentUser.getRole().equals(MemberRole.ADMIN)) {
         return Responses.PermissionProblem.ADMIN_ONLY;
      }

      if (dto.getRole() == null) {
         return new ServiceResponse("Role field cannot be empty!");
      }

      return memberRepository.findById(dto.getMemberId());
   }

}
