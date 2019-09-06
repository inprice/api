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
        if (id != null && id > 0 && id < Integer.MAX_VALUE) {
            return repository.findById(id);
        }
        return InstantResponses.INVALID_DATA("user!");
    }

    public ServiceResponse getList() {
        return repository.getList();
    }

    public ServiceResponse insert(UserDTO userDTO) {
        if (userDTO != null) {
            ServiceResponse res = validateUser(userDTO, true);
            if (res.isOK()) {
                res = repository.insert(userDTO);
                if (res.isOK()) {
                    log.info("A new user has been added successfully. CompanyId: {}, Email: {}", Context.getCompanyId(), userDTO.getEmail());
                }
            }
            return res;
        }
        return InstantResponses.INVALID_DATA("user!");
    }

    public ServiceResponse update(UserDTO userDTO) {
        if (userDTO != null && userDTO.getId() != null) {
            ServiceResponse res = validateUser(userDTO, false);
            if (res.isOK()) {
                res = repository.update(userDTO, true, true);
            }
            return res;
        }
        return InstantResponses.INVALID_DATA("user!");
    }

    public ServiceResponse updatePassword(PasswordDTO passwordDTO) {
        if (passwordDTO != null && passwordDTO.getId() != null) {
            ServiceResponse res = validatePassword(passwordDTO);
            if (res.isOK()) {
                res = repository.updatePassword(passwordDTO, Context.getAuthUser());
            }
            return res;
        }
        return InstantResponses.INVALID_DATA("password!");
    }

    public ServiceResponse deleteById(Long id) {
        if (id != null && id > 0 && id < Integer.MAX_VALUE) {
            return repository.deleteById(id);
        }
        return InstantResponses.INVALID_DATA("user!");
    }

    public ServiceResponse toggleStatus(Long id) {
        if (id != null && id > 0 && id < Integer.MAX_VALUE) {
            return repository.toggleStatus(id);
        }
        return InstantResponses.INVALID_DATA("user!");
    }

    private ServiceResponse validateUser(UserDTO userDTO, boolean insert) {
        List<Problem> problems = UserDTOValidator.verify(userDTO, insert, "Full");

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
