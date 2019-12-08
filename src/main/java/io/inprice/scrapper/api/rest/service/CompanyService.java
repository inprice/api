package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Commons;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.repository.CompanyRepository;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;
import io.inprice.scrapper.common.meta.Role;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;

import java.util.ArrayList;
import java.util.List;

public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private static final CompanyRepository repository = Beans.getSingleton(CompanyRepository.class);
    private static final AuthService authService = Beans.getSingleton(AuthService.class);

    public ServiceResponse findById(Long id) {
        return repository.findById(id);
    }

    public ServiceResponse insert(CompanyDTO companyDTO, Response response) {
        if (companyDTO != null) {
            ServiceResponse res = validate(companyDTO, true);
            if (res.isOK()) {
                res = repository.insert(companyDTO);
                if (res.isOK()) {
                    log.info("A new company has been added successfully. " + companyDTO);
                    LoginDTO loginDTO = new LoginDTO();
                    loginDTO.setEmail(companyDTO.getEmail());
                    loginDTO.setPassword(companyDTO.getPassword());
                    res = authService.login(loginDTO, response);
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
        if (! insert && (! Role.admin.equals(Context.getAuthUser().getRole()) || ! companyDTO.getId().equals(Context.getCompanyId()))) {
            return Responses.PermissionProblem.UNAUTHORIZED;
        }

        List<Problem> problems = new ArrayList<>();

        if (insert) {
            problems = UserDTOValidator.verify(companyDTO, true, "Contact");
        }

        if (StringUtils.isBlank(companyDTO.getCompanyName())) {
            problems.add(new Problem("companyName", "Company name cannot be null!"));
        } else if (companyDTO.getCompanyName().length() < 3 || companyDTO.getCompanyName().length() > 250) {
            problems.add(new Problem("companyName", "Company name must be between 3 and 250 chars!"));
        }

        if (companyDTO.getCountry() == null) {
            problems.add(new Problem("country", "You should pick a country!"));
        } else  if (StringUtils.isBlank(companyDTO.getCountry())) {
            problems.add(new Problem("country", "Unknown country!"));
        }

        if (companyDTO.getSector() == null) {
            problems.add(new Problem("sector", "You should pick a sector!"));
        } else  if (StringUtils.isBlank(companyDTO.getSector())) {
            problems.add(new Problem("sector", "Unknown sector!"));
        }

        return Commons.createResponse(problems);
    }

}
