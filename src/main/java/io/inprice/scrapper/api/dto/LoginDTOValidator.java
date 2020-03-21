package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.LoginDTO;

import java.util.List;

public class LoginDTOValidator {

    public static List<String> verify(LoginDTO loginDTO) {
        List<String> problems = PasswordDTOValidator.verify(loginDTO, false, false);
        EmailDTOValidator.verify(loginDTO.getEmail(), problems);
        return problems;
    }

}
