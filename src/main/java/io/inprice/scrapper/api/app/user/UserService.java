package io.inprice.scrapper.api.app.user;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.app.auth.AuthRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.dto.StringDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;

/**
 * TODO:
 * Eklenmesi gereken fonksiyonlar
 * 1- Bir davete internal olarak confirm ya da reject verilebilmeli
 */
public class UserService {

   private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private static final AuthRepository authRepository = Beans.getSingleton(AuthRepository.class);

   public ServiceResponse updateName(StringDTO dto) {
      if (StringUtils.isBlank(dto.getValue())) {
         return Responses.Invalid.NAME;
      } else if (dto.getValue().length() < 3 || dto.getValue().length() > 70) {
         return new ServiceResponse("Name must be between 3 and 70 chars!");
      }

      return userRepository.updateName(dto.getValue());
   }

   public ServiceResponse updatePassword(PasswordDTO dto) {
      String problem = PasswordValidator.verify(dto, true, true);
      if (problem == null) {
         return userRepository.updatePassword(dto.getPassword());
      } else {
         return new ServiceResponse(problem);
      }
   }

   public ServiceResponse getOpenedSessions() {
      return authRepository.findByUserId(CurrentUser.getUserId());
   }

   public ServiceResponse closeAllSessions() {
      if (authRepository.closeByUserId(CurrentUser.getUserId())) {
         return Responses.OK;
      }
      return Responses.ServerProblem.EXCEPTION;
   }

}
