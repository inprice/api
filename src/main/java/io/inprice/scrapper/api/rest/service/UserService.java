package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.repository.CompanyRepository;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.repository.WorkspaceRepository;
import io.inprice.scrapper.api.rest.validator.PasswordDTOValidator;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;
import io.inprice.scrapper.common.models.Workspace;
import org.eclipse.jetty.http.HttpStatus;

import java.util.List;

public class UserService {

    private final UserRepository repository = Beans.getSingleton(UserRepository.class);
    private final WorkspaceRepository workspaceRepository = Beans.getSingleton(WorkspaceRepository.class);

    public ServiceResponse findById(AuthUser authUser, Long id) {
        return repository.findById(authUser, id);
    }

    public ServiceResponse update(AuthUser authUser, UserDTO userDTO) {
        if (userDTO.getId() == null || userDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("User");
        }

        ServiceResponse res = validate(authUser, userDTO);
        if (res.isOK()) {
            res = repository.update(authUser, userDTO, false, false);
        }
        return res;
    }

    public ServiceResponse updatePassword(AuthUser authUser, PasswordDTO passwordDTO) {
        if (passwordDTO.getId() == null || passwordDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("User");
        }

        ServiceResponse res = validate(authUser, passwordDTO);
        if (res.isOK()) {
            res = repository.updatePassword(authUser, passwordDTO);
        }
        return res;
    }

    public ServiceResponse setDefaultWorkspace(AuthUser authUser, Long wsId) {
        ServiceResponse<Workspace> found = workspaceRepository.findById(authUser, wsId);
        if (found.isOK()) {
            return repository.setDefaultWorkspace(authUser, wsId);
        } else {
            return InstantResponses.NOT_FOUND("Workspace");
        }
    }

    private ServiceResponse validate(AuthUser authUser, PasswordDTO passwordDTO) {
        List<Problem> problems = PasswordDTOValidator.verify(authUser, passwordDTO, true, true);

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

    private ServiceResponse validate(AuthUser authUser, UserDTO userDTO) {
        List<Problem> problems = UserDTOValidator.verify(authUser, userDTO, false, "Full");

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        }
        return InstantResponses.OK;
    }

}
