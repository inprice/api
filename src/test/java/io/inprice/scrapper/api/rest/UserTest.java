package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Config;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class UserTest {

    private static final Config config = Beans.getSingleton(Config.class);

    @BeforeClass
    public static void setup() {
        Application.main(null);

        //insert a default company
        given()
            .port(config.getAPP_Port())
            .body(createAValidCompany())
        .post("/company");
    }

    @AfterClass
    public static void teardown() {
        Application.shutdown();
    }

    @Test
    public void everything_should_be_ok_with_insert() {
        final UserDTO user = createAValidUser();
        user.setEmail("test@test.com");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void invalid_data_for_user() {
        given()
            .port(config.getAPP_Port())
            .body("wrong body!").
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Invalid data for user!"));
    }

    @Test
    public void user_name_cannot_be_null() {
        final UserDTO user = createAValidUser();
        user.setFullName(null);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("User name cannot be null!"));
    }

    @Test
    public void user_name_length_is_out_of_range_if_less_than_3() {
        final UserDTO user = createAValidUser();
        user.setFullName("A");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("User name must be between 2 and 150 chars!"));
    }

    @Test
    public void user_name_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = createAValidUser();
        user.setFullName(StringUtils.repeat('A', 251));

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("User name must be between 2 and 150 chars!"));
    }

    @Test
    public void user_not_found_for_a_wrong_id_when_updated() {
        final UserDTO user = createAValidUser();
        user.setId(0L);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put("/user").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void contact_name_cannot_be_null() {
        final UserDTO user = createAValidUser();
        user.setFullName(null);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("User name cannot be null!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_less_than_2() {
        final UserDTO user = createAValidUser();
        user.setFullName("A");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("User name must be between 2 and 150 chars!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_greater_than_150() {
        final UserDTO user = createAValidUser();
        user.setFullName(StringUtils.repeat('A', 151));

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("User name must be between 2 and 150 chars!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final UserDTO user = createAValidUser();
        user.setEmail(null);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final String email = "mustafa@inprice.com";

        final UserDTO user = createAValidUser();
        user.setEmail(email);

        given()
            .port(config.getAPP_Port())
            .body(user)
        .post("/user");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo(email + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_9() {
        final UserDTO user = createAValidUser();
        user.setEmail("jd@in.io");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = createAValidUser();
        user.setEmail(StringUtils.repeat('a', 251));

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_is_invalid() {
        final UserDTO user = createAValidUser();
        user.setEmail("test@invalid");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void password_cannot_be_null() {
        final UserDTO user = createAValidUser();
        user.setPassword(null);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

    @Test
    public void password_length_is_out_of_range_if_less_than_5() {
        final UserDTO user = createAValidUser();
        user.setPassword("pass");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_length_is_out_of_range_if_greater_than_16() {
        final UserDTO user = createAValidUser();
        user.setPassword(StringUtils.repeat('a', 17));

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_are_mismatch() {
        final UserDTO user = createAValidUser();
        user.setPasswordAgain("password"); // --> password is p4ssw0rd

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post("/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Passwords are mismatch!"));
    }

    private UserDTO createAValidUser() {
        UserDTO user = new UserDTO();
        user.setFullName("John Doe");
        user.setEmail("jdoe@inprice.io");
        user.setPassword("p4ssw0rd");
        user.setPasswordAgain("p4ssw0rd");
        user.setCompanyId(1L);
        return user;
    }

    private static CompanyDTO createAValidCompany() {
        CompanyDTO company = new CompanyDTO();
        company.setCompanyName("inprice");
        company.setWebsite("www.inprice.io");
        company.setFullName("John Doe");
        company.setEmail("sample@inprice.io");
        company.setPassword("p4ssw0rd");
        company.setPasswordAgain("p4ssw0rd");
        company.setCountryId(1L);
        return company;
    }

}
