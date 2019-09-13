package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.validator.PasswordDTOValidator;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;
import org.eclipse.jetty.http.HttpStatus;

import java.util.List;

public class AdminService {

    private final UserRepository repository = Beans.getSingleton(UserRepository.class);

    public ServiceResponse update(UserDTO userDTO) {
        if (userDTO != null && userDTO.getId() != null && userDTO.getId() > 0) {

            ServiceResponse res = validateUser(userDTO);
            if (res.isOK()) {
                res = repository.update(userDTO, true, false);
            }

            return res;
        }
        return InstantResponses.INVALID_DATA("user data!");
    }

    public ServiceResponse updatePassword(PasswordDTO passwordDTO) {
        if (passwordDTO != null && passwordDTO.getId() != null && passwordDTO.getId() > 0) {

            ServiceResponse res = validatePassword(passwordDTO);
            if (res.isOK()) {
                res = repository.updatePassword(passwordDTO, Context.getAuthUser());
            }

            return res;
        }

        return InstantResponses.INVALID_DATA("password data!");
    }

    private ServiceResponse validateUser(UserDTO userDTO) {
        List<Problem> problems = UserDTOValidator.verify(userDTO, false, "Full");

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

    private ServiceResponse validatePassword(PasswordDTO passwordDTO) {
        List<Problem> problems = PasswordDTOValidator.verify(passwordDTO, true, true);

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

}
