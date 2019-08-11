package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.common.meta.UserType;

public class DTOHelper {

    public static LoginDTO getLoginDTO() {
        LoginDTO login = new LoginDTO();
        login.setEmail("sample@inprice.io");
        login.setPassword("p4ssw0rd");
        return login;
    }

    public static UserDTO getUserDTO() {
        UserDTO user = new UserDTO();
        user.setId(2L);
        user.setType(UserType.USER);
        user.setFullName("John Doe");
        user.setEmail("jdoe@inprice.io");
        user.setPassword("p4ssw0rd");
        user.setPasswordAgain("p4ssw0rd");
        return user;
    }

    public static WorkspaceDTO getWorkspaceDTO() {
        WorkspaceDTO ws = new WorkspaceDTO();
        ws.setName("SECONDARY WS");
        ws.setPlanId(1L);
        return ws;
    }

    public static CompanyDTO getCompanyDTO() {
        CompanyDTO company = new CompanyDTO();
        company.setCompanyName("inprice");
        company.setWebsite("www.inprice.io");
        company.setFullName("John Doe");

        LoginDTO loginDTO = getLoginDTO();
        company.setEmail(loginDTO.getEmail());
        company.setPassword(loginDTO.getPassword());
        company.setPasswordAgain(loginDTO.getPassword());

        company.setCountryId(1L);
        return company;
    }

}
