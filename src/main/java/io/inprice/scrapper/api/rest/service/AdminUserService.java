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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository repository = Beans.getSingleton(UserRepository.class);

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse getList() {
        return repository.getList();
    }

    public ServiceResponse insert(UserDTO userDTO) {
        ServiceResponse res = validate(userDTO, true);
        if (res.isOK()) {
            res = repository.insert(userDTO);
            if (res.isOK()) {
                log.info("A new user has been added successfully. CompanyId: {}, Email: {}", Context.getCompanyId(), userDTO.getEmail());
            }
        }
        return res;
    }

    public ServiceResponse update(UserDTO userDTO, boolean passwordWillBeUpdated) {
        if (userDTO.getId() == null || userDTO.getId() < 1) {
            return InstantResponses.NOT_FOUND("User");
        }

        ServiceResponse res = validate(userDTO, false);
        if (res.isOK()) {
            res = repository.update(userDTO, true, passwordWillBeUpdated);
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

    public ServiceResponse deleteById(Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("User");
        }

        return repository.deleteById(id);
    }

    public ServiceResponse toggleStatus(Long id) {
        if (id == null || id < 1) {
            return InstantResponses.NOT_FOUND("User");
        }

        return repository.toggleStatus(id);
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

    private ServiceResponse validate(UserDTO userDTO, boolean insert) {
        List<Problem> problems = UserDTOValidator.verify(userDTO, insert, "Full");

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

}
