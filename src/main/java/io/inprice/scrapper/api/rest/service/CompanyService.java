package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.rest.repository.CompanyRepository;
import io.inprice.scrapper.api.rest.repository.CountryRepository;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);
    
    private final CompanyRepository repository = Beans.getSingleton(CompanyRepository.class);
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

        List<Problem> problems;

        if (insert) {
            problems = UserDTOValidator.verify(companyDTO, true, "Contact");
        } else {
            problems = new ArrayList<>();
        }

        /*
         * company controls
         */
        if (! insert && companyDTO.getId() == null) {
            res.setResult("Company not found!");
            return res;
        }

        if (StringUtils.isBlank(companyDTO.getCompanyName())) {
            problems.add(new Problem("companyName", "Company name cannot be null!"));
        } else if (companyDTO.getCompanyName().length() < 3 || companyDTO.getCompanyName().length() > 250) {
            problems.add(new Problem("companyName", "The length of name field must be between 3 and 250 chars!"));
        }

        if (companyDTO.getCountryId() == null) {
            problems.add(new Problem("country", "You should pick a country!"));
        } else if (countryRepository.findById(companyDTO.getCountryId()) == null) {
            problems.add(new Problem("country", "Unknown country!"));
        }

        if (problems.size() > 0) {
            res.setProblems(problems);
        } else if (res.getResult() == null) {
            res = Responses.OK;
        }

        return res;
    }

}
