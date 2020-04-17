package io.inprice.scrapper.api.app.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.app.user_company.UserCompany;
import io.inprice.scrapper.api.app.user_company.UserCompanyRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;

/**
 * TODO:
 * Eklenmesi gereken fonksiyonlar
 * 1- Bir davete internal olarak confirm ya da reject verilebilmeli
 */
public class UserService {

   private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private static final UserCompanyRepository userCompanyRepository = Beans.getSingleton(UserCompanyRepository.class);

   public ServiceResponse updateName(String name) {
      if (StringUtils.isNotBlank(name)) {
         return Responses.Invalid.NAME;
      } else if (name.length() < 3 || name.length() > 70) {
         return new ServiceResponse("Name must be between 3 and 70 chars!");
      }

      return userRepository.updateName(name);
   }

   public ServiceResponse updatePassword(PasswordDTO dto) {
      String problem = PasswordValidator.verify(dto, true, true);
      if (problem == null) {
         return userRepository.updatePassword(dto.getPassword());
      } else {
         return new ServiceResponse(problem);
      }
   }

   public ServiceResponse getCompanyList() {
      ServiceResponse res = userCompanyRepository.getListByUser();
      if (res.isOK()) {
         List<UserCompany> members = res.getData();
         if (members != null && members.size() > 0) {
            List<Map<String, Object>> data = new ArrayList<>();
            for (UserCompany member: members) {
               Map<String, Object> props = new HashMap<>(6);
               props.put("id", member.getId());
               props.put("companyName", member.getCompanyName());
               props.put("role", member.getRole());
               props.put("status", member.getStatus());
               props.put("companyId", member.getCompanyId());
               props.put("updatedAt", member.getUpdatedAt());
               props.put("createdAt", member.getCreatedAt());

               data.add(props);
            }
            res = new ServiceResponse(data);
         }
      }
      return res;
   }

}
