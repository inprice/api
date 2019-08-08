package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.info.InstantResponses;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.repository.WorkspaceRepository;
import io.inprice.scrapper.api.rest.validator.PasswordDTOValidator;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;
import io.inprice.scrapper.common.models.Workspace;
import org.eclipse.jetty.http.HttpStatus;

import java.util.List;

public class AdminService {

    private final UserRepository repository = Beans.getSingleton(UserRepository.class);
    private final WorkspaceRepository workspaceRepository = Beans.getSingleton(WorkspaceRepository.class);

    public ServiceResponse update(AuthUser claims, UserDTO userDTO) {
        ServiceResponse res = validate(userDTO);
        if (res.isOK()) {
            res = repository.update(claims, userDTO, true, false);
        }
        return res;
    }

    public ServiceResponse updatePassword(AuthUser claims, PasswordDTO passwordDTO) {
        ServiceResponse res = validate(passwordDTO);
        if (res.isOK()) {
            res = repository.updatePassword(claims, passwordDTO);
        }
        return res;
    }

    public ServiceResponse setDefaultWorkspace(AuthUser claims, Long wsId) {
        ServiceResponse<Workspace> found = workspaceRepository.findById(claims, wsId);
        if (found.isOK()) {
            return repository.setDefaultWorkspace(claims, wsId);
        } else {
            return InstantResponses.NOT_FOUND("Workspace");
        }
    }

    private ServiceResponse validate(PasswordDTO passwordDTO) {
        List<Problem> problems = PasswordDTOValidator.verify(passwordDTO, true);

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
        } else {
            return InstantResponses.OK;
        }
    }

}
