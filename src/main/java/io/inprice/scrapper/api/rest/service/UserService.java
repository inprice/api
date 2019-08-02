package io.inprice.scrapper.api.rest.service;

import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.info.Responses;
import io.inprice.scrapper.api.rest.repository.CompanyRepository;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.api.rest.validator.UserDTOValidator;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);
    private final UserRepository repository = Beans.getSingleton(UserRepository.class);

    public Response findById(Long id) {
        return repository.findById(id, false);
    }

    public Response update(UserDTO userDTO) {
        Response res = validate(userDTO);
        if (res.isOK()) {
            res = repository.updateByUser(userDTO);
        }
        return res;
    }

    private Response validate(UserDTO userDTO) {
        List<Problem> problems = UserDTOValidator.verify(userDTO, false, "User");

        if (userDTO.getCompanyId() == null || userDTO.getCompanyId().intValue() < 1) {
            problems.add(new Problem("form", "Company cannot be null!"));
        } else if (! companyRepository.findById(userDTO.getCompanyId()).isOK()) {
            problems.add(new Problem("form", "Unknown company info!"));
        }

        if (problems.size() > 0) {
            Response res = new Response(HttpStatus.BAD_REQUEST_400);
            res.setProblems(problems);
            return res;
        }
        return Responses.OK;
    }

}
