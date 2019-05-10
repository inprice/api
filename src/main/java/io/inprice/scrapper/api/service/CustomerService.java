package io.inprice.scrapper.api.service;

import io.inprice.crawler.common.meta.UserType;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.models.Customer;
import io.inprice.scrapper.api.repository.CustomerRepository;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomerService {

    private static CustomerService service;
    private static CustomerRepository repository;

    public static CustomerService get() {
        if (service == null) {
            synchronized (CustomerService.class) {
                if (service == null) {
                    service = new CustomerService();
                    repository = new CustomerRepository();
                }
            }
        }

        return service;
    }

    public Customer signup(String body, UserType userType) {
        Customer json = Global.gson.fromJson(body, Customer.class);
        Customer customer = validate(json, userType);
        if (customer.getHttpStatus() == HttpStatus.OK_200) {
            return repository.signup(customer);
        }

        return customer;
    }

    private Customer validate(Customer cust, UserType userType) {
        if (cust.getId() != null) {
            return new Customer(HttpStatus.CONFLICT_409, "Customer has already been defined!");
        }

        List<String> problems = new ArrayList<>();

        if (StringUtils.isBlank(cust.getEmail())) problems.add("Email address cannot be null!");
        if (StringUtils.isNotBlank(cust.getEmail()) && ! isEmailValid(cust.getEmail())) problems.add("Email address is invalid!");
        if (StringUtils.isBlank(cust.getPassword()) || cust.getPassword().trim().length() < 4) problems.add("The length of password should between 4 and 12!");
        if (StringUtils.isBlank(cust.getCompanyName())) problems.add("Company name cannot be null!");
        if (StringUtils.isBlank(cust.getContactName())) problems.add("Contact name cannot be null!");
        if (StringUtils.isBlank(cust.getWebsite())) problems.add("Website cannot be null!");
        if (cust.getCountryId() == null || cust.getCountryId().intValue() < 1)problems.add("A country should be selected!");

        if (problems.size() > 0) {
            cust = new Customer(HttpStatus.BAD_REQUEST_400, problems);
        } else {
            cust.setUserType(userType);
            cust.setProblems(null);
        }

        return cust;
    }

    private boolean isEmailValid(String email) {
        final Pattern pattern = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(email);
        return m.matches();
    }

}
