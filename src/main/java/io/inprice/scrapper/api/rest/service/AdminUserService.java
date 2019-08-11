package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.AuthUser;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.info.InstantResponses;
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

    public ServiceResponse findById(AuthUser authUser, Long id) {
        return repository.findById(authUser, id);
    }

    public ServiceResponse getList(AuthUser authUser) {
        return repository.getList(authUser);
    }

    public ServiceResponse insert(AuthUser authUser, UserDTO userDTO) {
        ServiceResponse res = validate(authUser, userDTO, true);
        if (res.isOK()) {
            res = repository.insert(authUser, userDTO);
            if (res.isOK()) {
                log.info("A new user has been added successfully. CompanyId: {}, Email: {}", authUser.getCompanyId(), userDTO.getEmail());
            }
        }
        return res;
    }

    public ServiceResponse update(AuthUser authUser, UserDTO userDTO, boolean passwordWillBeUpdated) {
        ServiceResponse res = validate(authUser, userDTO, false);
        if (res.isOK()) {
            res = repository.update(authUser, userDTO, true, passwordWillBeUpdated);
        }
        return res;
    }

    public ServiceResponse updatePassword(AuthUser authUser, PasswordDTO passwordDTO) {
        ServiceResponse res = validate(authUser, passwordDTO);
        if (res.isOK()) {
            res = repository.updatePassword(authUser, passwordDTO);
        }
        return res;
    }

    public ServiceResponse deleteById(AuthUser authUser, Long id) {
        return repository.deleteById(authUser, id);
    }

    public ServiceResponse toggleStatus(AuthUser authUser, Long id) {
        return repository.toggleStatus(authUser, id);
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

    private ServiceResponse validate(AuthUser authUser, UserDTO userDTO, boolean insert) {
        List<Problem> problems = UserDTOValidator.verify(authUser, userDTO, insert, "Full");

        if (problems.size() > 0) {
            ServiceResponse res = new ServiceResponse(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return InstantResponses.OK;
        }
    }

}
