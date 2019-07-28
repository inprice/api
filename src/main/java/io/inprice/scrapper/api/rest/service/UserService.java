package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import org.slf4j.Logger;
import io.inprice.scrapper.common.models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository repository = Beans.getSingleton(UserRepository.class);

    public Response findById(Long id) {
        return repository.findById(id, false);
    }

    public Response findByEmail(String email) {
        EmailValidator validator = EmailValidator.getInstance();
        if (validator.isValid(email)) {
            return repository.findByEmail(email, false);
        }
        return Responses.NOT_FOUND("User");
    }

    public Response insert(User user) {
        Response res = validate(user, true);
        if (res.isOK()) {
            Response found = findByEmail(user.getEmail());
            if (found.isOK()) {
                return new Response(HttpStatus.CONFLICT_409, "This email address is already taken by another user. Please use a different one!");
            }
            res = repository.insert(user);
            if (res.isOK()) {
                log.info("A new user has been added successfully. Name: {}, CompanyId: {}", user.getName(), user.getCompanyId());
            }
        }
        return res;
    }

    public Response update(User user) {
        Response res = validate(user, false);
        if (res.isOK()) {
            Response found = findByEmail(user.getEmail());
            if (! found.isOK()) {
                return Responses.NOT_FOUND("User");
            }
            res = repository.update(user);
        }
        return res;
    }

    //todo: a dto class should be used here
    private Response validate(User cust, boolean insert) {
        List<Problem> problems = new ArrayList<>();

        if (! EmailValidator.getInstance().isValid(cust.getEmail())) {
            problems.add(new Problem("email", "Email address is invalid!"));
        }

        //todo: passwordAgain should be added and cheked
        if (StringUtils.isBlank(cust.getPassword()) || cust.getPassword().trim().length() < 4) {
            problems.add(new Problem("password", "Password length of should between 4 and 12!"));
        }

        if (cust.getCompanyId() == null || cust.getCompanyId().intValue() < 1) {
            problems.add(new Problem("form", "Company cannot be null!"));
        }

        if (problems.size() > 0) {
            Response res = new Response(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        } else {
            return Responses.OK;
        }
    }

}
