package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.rest.repository.CompanyRepository;
import io.inprice.scrapper.api.rest.repository.CountryRepository;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);
    
    private final CompanyRepository repository = Beans.getSingleton(CompanyRepository.class);
    private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
    private final CountryRepository countryRepository = Beans.getSingleton(CountryRepository.class);

    public Response findById(Long id) {
        return repository.findById(id);
    }

    public Response insert(CompanyDTO companyDTO) {
        Response res = validate(companyDTO, true);
        if (res.isOK()) {
            res = repository.insert(companyDTO);
            if (res.isOK()) {
                log.info("A new company has been added successfully. " + companyDTO);
            }
        }
        return res;
    }

    public Response update(CompanyDTO companyDTO) {
        Response res = validate(companyDTO, false);
        if (res.isOK()) {
            res = repository.update(companyDTO);
        }
        return res;
    }

    private Response validate(CompanyDTO companyDTO, boolean insert) {
        Response res = new Response(HttpStatus.BAD_REQUEST_400);

        List<Problem> problems = new ArrayList<>();

        /*
         * company controls
         */
        if (! insert && companyDTO.getId() == null) {
            res.setResult("Company not found!");
            return res;
        }

        if (StringUtils.isBlank(companyDTO.getName())) {
            problems.add(new Problem("companyName", "Company name cannot be null!"));
        } else if (companyDTO.getName().length() < 3 || companyDTO.getName().length() > 250) {
            problems.add(new Problem("companyName", "The length of name field must be between 3 and 250 chars!"));
        }

        if (companyDTO.getCountryId() == null) {
            problems.add(new Problem("country", "You should pick a country!"));
        } else if (countryRepository.findById(companyDTO.getCountryId()) == null) {
            problems.add(new Problem("country", "Unknown country!"));
        }

        /*
         * contact controls
         */
        if (insert) {
            if (StringUtils.isBlank(companyDTO.getContactName())) {
                problems.add(new Problem("contactName", "Contact name cannot be null!"));
            } else if (companyDTO.getContactName().length() < 2 || companyDTO.getContactName().length() > 150) {
                problems.add(new Problem("contactName", "Contact name must be between 2 and 150 chars!"));
            }

            final String email = companyDTO.getContactEmail();
            if (StringUtils.isBlank(email)) {
                problems.add(new Problem("contactEmail", "Email address cannot be null!"));
            } else if (email.length() < 9 || email.length() > 250) {
                problems.add(new Problem("contactEmail", "Email address must be between 9 and 250 chars!"));
            } else if (!EmailValidator.getInstance().isValid(email)) {
                problems.add(new Problem("contactEmail", "Invalid email address!"));
            } else if (userRepository.findByEmail(email, false).isOK()) {
                problems.add(new Problem("contactEmail", email + " is already used by another user!"));
            }

            if (StringUtils.isBlank(companyDTO.getPassword())) {
                problems.add(new Problem("password", "Password cannot be null!"));
            } else if (companyDTO.getPassword().length() < 5 || companyDTO.getPassword().length() > 16) {
                problems.add(new Problem("password", "Password length must be between 5 and 16 chars!"));
            } else if (!companyDTO.getPassword().equals(companyDTO.getPasswordAgain())) {
                problems.add(new Problem("password", "Passwords are mismatch!"));
            }
        }

        if (problems.size() > 0) {
            res.setProblems(problems);
        } else if (res.getResult() == null) {
            res = Responses.OK;
        }

        return res;
    }

}
