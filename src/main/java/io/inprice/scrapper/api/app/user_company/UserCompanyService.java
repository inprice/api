package io.inprice.scrapper.api.app.user_company;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.auth.AuthRepository;
import io.inprice.scrapper.api.app.user.UserRole;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.MemberChangeRoleDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

public class UserCompanyService {

   private static final Logger log = LoggerFactory.getLogger(UserCompanyService.class);

   private final AuthRepository authRepository = Beans.getSingleton(AuthRepository.class);
   private final UserCompanyRepository userCompanyRepository = Beans.getSingleton(UserCompanyRepository.class);

   public ServiceResponse getList() {
      return userCompanyRepository.getListByCompany();
   }

   public ServiceResponse deleteById(Long id) {
      if (id != null && id > 0) {
         UserCompany userCompany = userCompanyRepository.getById(id);
         if (userCompany != null) {
            ServiceResponse res = userCompanyRepository.deleteById(userCompany.getId());
            if (res.isOK()) {
               authRepository.closeByUserAndCompanyId(userCompany.getUserId(), userCompany.getCompanyId());
               return Responses.OK;
            }
         }
      }
      return Responses.NotFound.INVITATION;
   }

   public ServiceResponse changeRole(MemberChangeRoleDTO dto, String ip, String userAgent) {
      ServiceResponse res = validate(dto);
      if (res.isOK()) {

         UserCompany userCompany = res.getData();
         if (! userCompany.getRole().equals(UserRole.ADMIN)) {
            res = userCompanyRepository.changeRole(dto);
            if (res.isOK()) {
               authRepository.closeByUserAndCompanyId(userCompany.getUserId(), CurrentUser.getCompanyId());
               log.info("{} role is changed to {} ", dto.getMemberId(), userCompany.getRole());
            }
         } else {
            res = new ServiceResponse("Admin's role cannot be changed!");
         }
      }
      return res;
   }

   private ServiceResponse validate(MemberChangeRoleDTO dto) {
      if (dto == null || dto.getMemberId() == null) {
         return new ServiceResponse("UserCompany Id field cannot be empty!");
      }

      if (! CurrentUser.getRole().equals(UserRole.ADMIN)) {
         return Responses.PermissionProblem.ADMIN_ONLY;
      }

      if (dto.getRole() == null) {
         return new ServiceResponse("Role field cannot be empty!");
      }

      return userCompanyRepository.findById(dto.getMemberId());
   }

}
