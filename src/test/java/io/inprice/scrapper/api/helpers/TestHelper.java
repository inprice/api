package io.inprice.scrapper.api.helpers;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.*;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.common.meta.UserType;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import org.eclipse.jetty.http.HttpStatus;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

public class TestHelper {

    private static final Properties properties = Beans.getSingleton(Properties.class);
    private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    public static void setup(boolean defaultCompanyAndUser, boolean extraUser) {
        setup(defaultCompanyAndUser, extraUser, false);
    }

    public static void setup(boolean defaultCompanyAndUser, boolean extraUser, boolean addWorkspaceCookie) {
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

            loginAsAdmin(addWorkspaceCookie);

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

    public static ProductDTO getProductDTO() {
        ProductDTO product = new ProductDTO();
        product.setCode("AX-123");
        product.setName("120x150 Dinner Table");
        product.setBrand("Wooden");
        product.setPrice(BigDecimal.valueOf(420.00));
        return product;
    }

    public static LinkDTO getLinkDTO(Long prodId) {
        LinkDTO link = new LinkDTO(prodId);
        link.setUrl("https://www.amazon.com/dp/1234567890");
        return link;
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

    public static void loginAsAdmin() {
        loginAsAdmin(false);
    }

    public static void loginAsAdmin(boolean addWorkspaceCookie) {
        RestAssured.requestSpecification = null;

        //dont forget, the id 1 is reserved for the admin only during testing
        Response res =
            given()
                .body(TestHelper.getLoginDTO()).
            when()
                .post(Consts.Paths.Auth.LOGIN).
            then()
                .statusCode(HttpStatus.OK_200).assertThat()
            .extract().
        response();

        RestAssured.requestSpecification =
            new RequestSpecBuilder()
                .addCookie(Consts.Auth.WORKSPACE_COOKIE, addWorkspaceCookie ? "1" : null)
                .addHeader(Consts.Auth.AUTHORIZATION_HEADER, res.header(Consts.Auth.AUTHORIZATION_HEADER))
            .build();
    }

    public static void loginAsUser() {
        loginAsUser(false);
    }

    public static void loginAsUser(boolean addWorkspaceCookie) {
        RestAssured.requestSpecification = null;

        //dont forget, the id 1 is reserved for the admin only during testing
        Response res =
            given()
                .body(TestHelper.getUserDTO()).
            when()
                .post(Consts.Paths.Auth.LOGIN).
            then()
                .statusCode(HttpStatus.OK_200).assertThat()
            .extract().
        response();

        RestAssured.requestSpecification =
            new RequestSpecBuilder()
                .addCookie(Consts.Auth.WORKSPACE_COOKIE, addWorkspaceCookie ? "1" : null)
                .addHeader(Consts.Auth.AUTHORIZATION_HEADER, res.header(Consts.Auth.AUTHORIZATION_HEADER))
            .build();
    }

    public static void logout() {
        when()
            .post(Consts.Paths.Auth.LOGOUT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();
    }

    public static int getTTL_TokensInSeconds() {
        return properties.getTTL_TokensInSeconds();
    }

}
