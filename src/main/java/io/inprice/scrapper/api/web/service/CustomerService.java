package io.inprice.scrapper.api.web.service;

import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.framework.Service;
import io.inprice.scrapper.api.web.repository.CustomerRepository;
import io.inprice.scrapper.common.logging.Logger;
import io.inprice.scrapper.common.models.Customer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.eclipse.jetty.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerService {

    private static final Logger log = new Logger(CustomerService.class);

    public Response findById(Long id) {
        return getRepository().findById(id, false);
    }

    public Response findByEmail(String email) {
        EmailValidator validator = EmailValidator.getInstance();
        if (validator.isValid(email)) {
            return getRepository().findByEmail(email, false);
        }
        return Responses.NOT_FOUND("Customer");
    }

    public Response insert(Customer customer) {
        Response res = validate(customer, true);
        if (res.isOK()) {
            Response found = findByEmail(customer.getEmail());
            if (found.isOK()) {
                return new Response(HttpStatus.CONFLICT_409, "This email address is already taken by another customer. Please use a different one!");
            }
            getRepository().insert(customer);
        }
        return res;
    }

    public Response update(Customer customer) {
        Response res = validate(customer, false);
        if (res.isOK()) {
            Response found = findByEmail(customer.getEmail());
            if (! found.isOK()) {
                return Responses.NOT_FOUND("Customer");
            }
            getRepository().update(customer);
        }
        return res;
    }

    private Response validate(Customer cust, boolean insert) {
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

    private CustomerRepository repository;

    private CustomerRepository getRepository() {
        if (repository == null) {
            synchronized (log) {
                if (repository == null) {
                    repository = new CustomerRepository();
                }
            }
        }
        return repository;
    }

}
