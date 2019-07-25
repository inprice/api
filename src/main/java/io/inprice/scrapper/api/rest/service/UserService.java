package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.eclipse.jetty.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

public class UserService {

    private static final Logger log = new Logger(UserService.class);
    
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
                log.info("A new user has been added successfully. Name: %s, CompanyId: %d", user.getName(), user.getCompanyId());
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

    private Response validate(User cust, boolean insert) {
        List<String> problems = new ArrayList<>();

        if (! EmailValidator.getInstance().isValid(cust.getEmail())) problems.add("Email address is invalid!");
        if (insert && (StringUtils.isBlank(cust.getPassword()) || cust.getPassword().trim().length() < 4)) problems.add("Password length of should between 4 and 12!");
        if (cust.getCompanyId() == null || cust.getCompanyId().intValue() < 1)problems.add("Company cannot be null!");

        if (problems.size() > 0) {
            Response res = new Response(HttpStatus.BAD_REQUEST_400);
            res.setProblemList(problems);
            return res;
        } else {
            return Responses.OK;
        }
    }

}
