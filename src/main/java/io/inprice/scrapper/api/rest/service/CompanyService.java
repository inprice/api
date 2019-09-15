package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.repository.CompanyRepository;
import io.inprice.scrapper.api.rest.repository.CountryRepository;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;
import io.inprice.scrapper.common.meta.UserType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);
    private static final CompanyRepository repository = Beans.getSingleton(CompanyRepository.class);
    private static final CountryRepository countryRepository = Beans.getSingleton(CountryRepository.class);

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse insert(CompanyDTO companyDTO) {
        if (companyDTO != null) {
            ServiceResponse res = validate(companyDTO, true);
            if (res.isOK()) {
                res = repository.insert(companyDTO);
                if (res.isOK()) {
                    log.info("A new company has been added successfully. " + companyDTO);
                }
            }
            return res;
        }
        return Responses.Invalid.COMPANY;
    }

    public ServiceResponse update(CompanyDTO companyDTO) {
        if (companyDTO != null) {
            if (companyDTO.getId() == null || companyDTO.getId() < 1) {
                return Responses.NotFound.COMPANY;
            }

            ServiceResponse res = validate(companyDTO, false);
            if (res.isOK()) {
                res = repository.update(companyDTO);
            }
            return res;
        }
        return Responses.Invalid.COMPANY;
    }

    private ServiceResponse validate(CompanyDTO companyDTO, boolean insert) {
        //only admins can update their companies
        if (! insert && UserType.ADMIN.equals(Context.getAuthUser().getType())) {
            return Responses.PermissionProblem.UNAUTHORIZED;
        }

        List<Problem> problems = new ArrayList<>();

        if (insert) {
            problems = UserDTOValidator.verify(companyDTO, true, "Contact");
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

        return Commons.createResponse(problems);
    }

}
