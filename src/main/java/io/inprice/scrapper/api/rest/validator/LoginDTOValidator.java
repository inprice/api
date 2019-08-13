package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.info.Problem;

import java.util.List;

public class LoginDTOValidator {

    public static List<Problem> verify(LoginDTO loginDTO) {
        List<Problem> problems = PasswordDTOValidator.verify(null, loginDTO, false, false);
        EmailDTOValidator.verify(loginDTO.getEmail(), problems);

        return problems;
    }

}
