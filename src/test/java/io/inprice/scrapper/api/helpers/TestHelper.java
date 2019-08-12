package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.common.meta.UserType;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import org.eclipse.jetty.http.HttpStatus;

import static io.restassured.RestAssured.given;

public class TestHelper {

    private static final Properties properties = Beans.getSingleton(Properties.class);
    private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public static void setup(boolean defaultCompanyAndUser, boolean extraUser) {
        if (Global.isApplicationRunning) {
            dbUtils.reset();
        } else {
            Application.main(null);
        }

        RestAssured.port = properties.getAPP_Port();

        if (defaultCompanyAndUser) {
            //insert a default company
            given()
                .body(TestHelper.getCompanyDTO()).
            when()
                .post(Consts.Paths.Company.REGISTER).
            then()
                .statusCode(HttpStatus.OK_200).assertThat();

            //be careful, the id 1 is reserved for the admin only during testing
            Response res =
                given()
                    .body(TestHelper.getLoginDTO()).
                when()
                    .post(Consts.Paths.Auth.LOGIN).
                then()
                    .extract().
                response();

            RestAssured.requestSpecification =
                new RequestSpecBuilder()
                    .addHeader(Consts.Auth.AUTHORIZATION_HEADER, res.header(Consts.Auth.AUTHORIZATION_HEADER))
                .build();

            //insert a user to use him
            if (extraUser) {
                given()
                    .body(TestHelper.getUserDTO()).
                when()
                    .post(Consts.Paths.AdminUser.BASE).
                then()
                    .statusCode(HttpStatus.OK_200).assertThat();
            }
        }
    }

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
