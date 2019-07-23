package io.inprice.scrapper.api.rest.user;

import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.eclipse.jetty.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

class Service {

    private static final Logger log = new Logger("UserService");
    
    private final Repository repository = Beans.getSingleton(Repository.class);

    Response findById(Long id) {
        return repository.findById(id, false);
    }

    Response findByEmail(String email) {
        EmailValidator validator = EmailValidator.getInstance();
        if (validator.isValid(email)) {
            return repository.findByEmail(email, false);
        }
        return Responses.NOT_FOUND("User");
    }

    Response insert(User user) {
        Response res = validate(user, true);
        if (res.isOK()) {
            Response found = findByEmail(user.getEmail());
            if (found.isOK()) {
                return new Response(HttpStatus.CONFLICT_409, "This email address is already taken by another user. Please use a different one!");
            }
            repository.insert(user);
        }
        return res;
    }

    Response update(User user) {
        Response res = validate(user, false);
        if (res.isOK()) {
            Response found = findByEmail(user.getEmail());
            if (! found.isOK()) {
                return Responses.NOT_FOUND("User");
            }
            repository.update(user);
        }
        return res;
    }

    private Response validate(User cust, boolean insert) {
        List<String> problems = new ArrayList<>();

        if (! EmailValidator.getInstance().isValid(cust.getEmail())) problems.add("Email address is invalid!");
        if (insert && (StringUtils.isBlank(cust.getPassword()) || cust.getPassword().trim().length() < 4)) problems.add("Password  length of should between 4 and 12!");
        if (StringUtils.isBlank(cust.getCompanyName())) problems.add("Company name cannot be null!");
        if (StringUtils.isBlank(cust.getContactName())) problems.add("Contact name cannot be null!");
        if (cust.getCountryId() == null || cust.getCountryId().intValue() < 1)problems.add("A country should be selected!");

        if (problems.size() > 0) {
            Response res = new Response(HttpStatus.BAD_REQUEST_400);
            res.setProblemList(problems);
            return res;
        } else {
            return Responses.OK;
        }
    }

}
