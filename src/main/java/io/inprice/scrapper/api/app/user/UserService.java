package io.inprice.scrapper.api.app.user;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.inprice.scrapper.api.app.auth.AuthRepository;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.dto.LongDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.PasswordValidator;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.session.info.ForCookie;
import io.inprice.scrapper.api.utils.Timezones;

/**
 * TODO:
 * Eklenmesi gereken fonksiyonlar
 * 1- Bir davete internal olarak confirm ya da reject verilebilmeli
 */
public class UserService {

   private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private static final AuthRepository authRepository = Beans.getSingleton(AuthRepository.class);

   public ServiceResponse update(UserDTO dto) {
    String problem = validateUserDTOForUpdate(dto);
      if (problem == null) {
        return userRepository.update(dto);
     } else {
        return new ServiceResponse(problem);
     }
   }

   public ServiceResponse updatePassword(PasswordDTO dto) {
      String problem = PasswordValidator.verify(dto, true, true);
      if (problem == null) {
         return userRepository.updatePassword(dto.getPassword());
      } else {
         return new ServiceResponse(problem);
      }
   }

   public ServiceResponse getInvitations() {
      return userRepository.findActiveInvitations();
   }

   public ServiceResponse acceptInvitation(LongDTO dto) {
      if (dto.getValue() == null || dto.getValue() < 1) {
         return Responses.Invalid.DATA;
      }
      return userRepository.acceptInvitation(dto.getValue());
   }

   public ServiceResponse rejectInvitation(LongDTO dto) {
      if (dto.getValue() == null || dto.getValue() < 1) {
         return Responses.Invalid.DATA;
      }
      return userRepository.rejectInvitation(dto.getValue());
   }

   public ServiceResponse getMemberships() {
      return userRepository.findMemberships();
   }

   public ServiceResponse leaveMembership(LongDTO dto) {
      if (dto.getValue() == null || dto.getValue() < 1) {
         return Responses.Invalid.DATA;
      }
      return userRepository.leaveMembership(dto.getValue());
   }

   public ServiceResponse getOpenedSessions(List<ForCookie> cookieSesList) {
      List<String> hashes = new ArrayList<>(cookieSesList.size());
      for (ForCookie ses: cookieSesList) {
         hashes.add(ses.getHash());
      }
      return authRepository.findOpenedSessions(hashes);
   }

   public ServiceResponse closeAllSessions() {
      if (authRepository.closeByUserId(CurrentUser.getUserId())) {
         return Responses.OK;
      }
      return Responses.ServerProblem.EXCEPTION;
   }

   private String validateUserDTOForUpdate(UserDTO dto) {
    String problem = null;

    if (dto == null) {
      problem = "Invalid user info!";
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getName())) {
        problem = "User name cannot be null!";
      } else if (dto.getName().length() < 3 || dto.getName().length() > 70) {
        problem = "User name must be between 3 - 70 chars";
      }
    }

    if (problem == null) {
      if (StringUtils.isBlank(dto.getTimezone())) {
        problem = "Time zone cannot be empty!";
      } else if (! Timezones.exists(dto.getTimezone())) {
        problem = "Unknown time zone!";
      }
    }

    return problem;
  }

}
