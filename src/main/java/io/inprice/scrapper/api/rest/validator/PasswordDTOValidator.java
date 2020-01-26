package io.inprice.scrapper.api.rest.validator;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.rest.component.Context;
import io.inprice.scrapper.api.rest.repository.UserRepository;
import io.inprice.scrapper.common.meta.Role;
import io.inprice.scrapper.common.models.User;
import jodd.util.BCrypt;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PasswordDTOValidator {

    private static final UserRepository userRepository = Beans.getSingleton(UserRepository.class);

    public static List<String> verify(PasswordDTO dto, boolean againPassCheck, boolean oldPassCheck) {
        List<String> problems = new ArrayList<>();

        if (StringUtils.isBlank(dto.getPassword())) {
            problems.add("Password cannot be null!");
        } else if (dto.getPassword().length() < 4 || dto.getPassword().length() > 16) {
            problems.add("Password length must be between 4 and 16 chars!");
        } else if (againPassCheck && ! dto.getPassword().equals(dto.getRepeatPassword())) {
            problems.add("Passwords are mismatch!");
        }

        if (oldPassCheck) {
            if (!Role.admin.equals(Context.getAuthUser().getRole()) && !dto.getId().equals(Context.getAuthUser().getId())) {
                problems.add("User has no permission to update password!");
            } else {
                if (StringUtils.isBlank(dto.getPasswordOld())) {
                    problems.add("Old password cannot be null!");
                } else if (problems.size() < 1) {
                    ServiceResponse found = userRepository.findById(dto.getId(), true);
                    User user = found.getData();
                    if (found.isOK()) {
                        final String hash = BCrypt.hashpw(dto.getPasswordOld(), user.getPasswordSalt());
                        if (!hash.equals(user.getPasswordHash())) {
                            problems.add("Old password is incorrect!");
                        }
                    }
                }
            }
        }

        return problems;
    }

}
