package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.validator.PasswordDTOValidator;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;

import java.util.List;

public class UserService {

    private final UserRepository repository = Beans.getSingleton(UserRepository.class);

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse update(UserDTO userDTO) {
        if (userDTO != null) {
            if (userDTO.getId() == null || userDTO.getId() < 1) {
                return Responses.NotFound.USER;
            }

            ServiceResponse res = validate(userDTO);
            if (res.isOK()) {
                res = repository.update(userDTO, false, false);
            }
            return res;
        }
        return Responses.Invalid.USER;
    }

    public ServiceResponse updatePassword(PasswordDTO passwordDTO) {
        if (passwordDTO != null) {
            if (passwordDTO.getId() == null || passwordDTO.getId() < 1) {
                return Responses.NotFound.USER;
            }

            ServiceResponse res = validate(passwordDTO);
            if (res.isOK()) {
                res = repository.updatePassword(passwordDTO, Context.getAuthUser());
            }
            return res;
        }
        return Responses.Invalid.PASSWORD;
    }

    private ServiceResponse validate(UserDTO userDTO) {
        List<Problem> problems = UserDTOValidator.verify(userDTO, false, "Full");
        return Commons.createResponse(problems);
    }

    private ServiceResponse validate(PasswordDTO passwordDTO) {
        List<Problem> problems = PasswordDTOValidator.verify(passwordDTO, true, true);
        return Commons.createResponse(problems);
    }

}
