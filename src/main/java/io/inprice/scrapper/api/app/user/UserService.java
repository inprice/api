package io.inprice.scrapper.api.app.user;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.app.member.Member;
import io.inprice.scrapper.api.app.member.MemberRepository;
import io.inprice.scrapper.api.app.member.MemberStatus;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;

/**
 * TODO:
 * Eklenmesi gereken fonksiyonlar
 * 1- updateLastCompany metodu icinde yeni refresh ve access token lar uretilip kullaniciya donulmeli
 * 2- Bir davete internal olarak confirm ya da reject verilebilmeli
 */
public class UserService {

   private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private static final MemberRepository memberRepository = Beans.getSingleton(MemberRepository.class);

   public ServiceResponse updateName(String name) {
      if (StringUtils.isNotBlank(name)) {
         return Responses.Invalid.NAME;
      } else if (name.length() < 3 || name.length() > 70) {
         return new ServiceResponse("Name must be between 3 and 70 chars!");
      }

      return userRepository.updateName(name);
   }

   public ServiceResponse changeActiveCompany(Long companyId) {
      if (companyId != null) {
         if (companyId == null || companyId < 1) {
            return Responses.NotFound.COMPANY;
         }

         ServiceResponse found = memberRepository.findByEmailAndCompanyId(CurrentUser.getEmail(), companyId);
         if (found.isOK()) {
            Member member = found.getData();
            if (MemberStatus.JOINED.equals(member.getStatus())) {
               return userRepository.updateLastCompany(companyId);
            }
         } else {
            return new ServiceResponse("Your membership to this company seems not activated yet. Please confirm it first!");
         }
      }
      return Responses.NotFound.COMPANY;
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
      ServiceResponse res = memberRepository.getListByUser();
      if (res.isOK()) {
         List<Member> members = res.getData();
         if (members != null && members.size() > 0) {
            List<Map<String, Object>> data = new ArrayList<>();
            for (Member member: members) {
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
