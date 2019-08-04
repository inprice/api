package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Claims;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
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

    public Response findById(Long id) {
        return repository.findById(id, false);
    }

    public Response getList(long companyId) {
        return repository.getList(companyId);
    }

    public Response insert(Claims claims, UserDTO userDTO) {
        Response res = validate(userDTO, true);
        if (res.isOK()) {
            res = repository.insert(userDTO);
            if (res.isOK()) {
                log.info("A new user has been added successfully. CompanyId: {}, Email: {}", userDTO.getCompanyId(), userDTO.getEmail());
            }
        }
        return res;
    }

    public Response update(Claims claims, UserDTO userDTO, boolean passwordWillBeUpdated) {
        Response res = validate(userDTO, false);
        if (res.isOK()) {
            res = repository.update(claims, userDTO, true, passwordWillBeUpdated);
        }
        return res;
    }

    public Response updatePassword(Claims claims, PasswordDTO passwordDTO) {
        Response res = validate(passwordDTO);
        if (res.isOK()) {
            res = repository.updatePassword(claims, passwordDTO);
        }
        return res;
    }

    public Response deleteById(Long id) {
        return repository.deleteById(id);
    }

    public Response toggleStatus(Long id) {
        return repository.toggleStatus(id);
    }

    private Response validate(PasswordDTO passwordDTO) {
        List<Problem> problems = PasswordDTOValidator.verify(passwordDTO, true);

        if (problems.size() > 0) {
            Response res = new Response(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return Responses.OK;
        }
    }

    private Response validate(UserDTO userDTO, boolean insert) {
        List<Problem> problems = UserDTOValidator.verify(userDTO, insert, "Full");

        if (problems.size() > 0) {
            Response res = new Response(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return Responses.OK;
        }
    }

}
