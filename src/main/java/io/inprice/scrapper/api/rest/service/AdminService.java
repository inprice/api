package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.rest.repository.CompanyRepository;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);
    private final UserRepository repository = Beans.getSingleton(UserRepository.class);

    public Response findById(Long id) {
        return repository.findById(id, false);
    }

    public Response getAll(long companyId) {
        return repository.getAll(companyId);
    }

    public Response insert(UserDTO userDTO) {
        Response res = validate(userDTO, true);
        if (res.isOK()) {
            res = repository.insert(userDTO);
            if (res.isOK()) {
                log.info("A new user has been added successfully. CompanyId: {}, Email: {}", userDTO.getCompanyId(), userDTO.getEmail());
            }
        }
        return res;
    }

    public Response update(UserDTO userDTO) {
        Response res = validate(userDTO, false);
        if (res.isOK()) {
            res = repository.updateByAdmin(userDTO);
        }
        return res;
    }

    public Response deleteById(Long id) {
        return repository.deleteById(id);
    }

    public Response toggleStatus(Long id) {
        return repository.toggleStatus(id);
    }

    private Response findByEmail(String email) {
        EmailValidator validator = EmailValidator.getInstance();
        if (validator.isValid(email)) {
            return repository.findByEmail(email, false);
        }
        return Responses.NOT_FOUND("User");
    }

    private Response validate(UserDTO userDTO, boolean insert) {
        List<Problem> problems = UserDTOValidator.verify(userDTO, insert, "User");

        if (userDTO.getCompanyId() == null || userDTO.getCompanyId().intValue() < 1) {
            problems.add(new Problem("form", "Company cannot be null!"));
        } else if (! companyRepository.findById(userDTO.getCompanyId()).isOK()) {
            problems.add(new Problem("form", "Unknown company info!"));
        }

        if (problems.size() > 0) {
            Response res = new Response(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            Response found = findByEmail(userDTO.getEmail());
            if (! insert && ! found.isOK()) {
                return Responses.NOT_FOUND("User");
            }
            if (insert && found.isOK()) {
                return new Response(HttpStatus.CONFLICT_409, "This email address is already taken by another user. Please use a different one!");
            }
            if (! insert && ! userDTO.getId().equals(found.getModel().getId())) {
                log.error("It looks like someone is attacking! " + userDTO.toString());
                return new Response(HttpStatus.CONFLICT_409, "Wrong data!");
            }
            return Responses.OK;
        }
    }

}
