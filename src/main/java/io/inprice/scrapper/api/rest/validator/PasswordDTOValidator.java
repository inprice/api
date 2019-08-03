package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.Problem;
import io.inprice.scrapper.api.info.Response;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.common.models.User;
import jodd.util.BCrypt;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PasswordDTOValidator {

    private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

    public static List<Problem> verify(PasswordDTO dto, boolean checkOldPassword) {
        List<Problem> problems = new ArrayList<>();

        if (StringUtils.isBlank(dto.getPassword())) {
            problems.add(new Problem("password", "Password cannot be null!"));
        } else if (dto.getPassword().length() < 5 || dto.getPassword().length() > 16) {
            problems.add(new Problem("password", "Password length must be between 5 and 16 chars!"));
        } else if (!dto.getPassword().equals(dto.getPasswordAgain())) {
            problems.add(new Problem("password", "Passwords are mismatch!"));
        }

        if (checkOldPassword && problems.size() < 1) {
            Response<User> user = userRepository.findById(dto.getId(), true);

            if (StringUtils.isBlank(dto.getPasswordOld())) {
                problems.add(new Problem("passwordOld", "Old password cannot be null!"));
            } else {
                final String hash = BCrypt.hashpw(dto.getPasswordOld(), user.getModel().getPasswordSalt());
                if (! hash.equals(user.getModel().getPasswordHash())) {
                    problems.add(new Problem("passwordOld", "Old password is incorrect!"));
                }
            }
        }

        return problems;
    }

}
