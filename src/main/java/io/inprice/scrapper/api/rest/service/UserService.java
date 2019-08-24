package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.repository.WorkspaceRepository;
import io.inprice.scrapper.api.rest.validator.PasswordDTOValidator;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;
import org.eclipse.jetty.http.HttpStatus;

import java.util.List;

public class UserService {

    private final UserRepository repository = Beans.getSingleton(UserRepository.class);
    private final WorkspaceRepository workspaceRepository = Beans.getSingleton(WorkspaceRepository.class);

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse update(UserDTO userDTO) {
        if (userDTO.getId() == null || userDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("User");
        }

        ServiceResponse res = validate(userDTO);
        if (res.isOK()) {
            res = repository.update(userDTO, false, false);
        }
        return res;
    }

    public ServiceResponse updatePassword(PasswordDTO passwordDTO) {
        if (passwordDTO.getId() == null || passwordDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("User");
        }

        ServiceResponse res = validate(passwordDTO);
        if (res.isOK()) {
            res = repository.updatePassword(passwordDTO, Context.getAuthUser());
        }
        return res;
    }

    private ServiceResponse validate(PasswordDTO passwordDTO) {
        List<Problem> problems = PasswordDTOValidator.verify(passwordDTO, true, true);

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

    private ServiceResponse validate(UserDTO userDTO) {
        List<Problem> problems = UserDTOValidator.verify(userDTO, false, "Full");

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        }
        return InstantResponses.OK;
    }

}
