package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.helpers.TestHelper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class UserTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true);
    }

    @Test
    public void everything_should_be_ok_with_updating() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setFullName("Jane Doe");
        user.setEmail("janed@inprice.io");

        given()
            .body(user).
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void invalid_data_for_user() {
        given()
            .body("wrong body!").
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.Invalid.USER.getStatus()));
    }

    @Test
    public void full_name_cannot_be_null() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setFullName(null);

        given()
            .body(user).
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Full name cannot be null!"));
    }

    @Test
    public void full_name_length_is_out_of_range_if_less_than_3() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setFullName("A");

        given()
            .body(user).
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Full name must be between 2 and 150 chars!"));
    }

    @Test
    public void full_name_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setFullName(StringUtils.repeat('A', 251));

        given()
            .body(user).
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Full name must be between 2 and 150 chars!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail(null);

        given()
            .body(user).
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final String email = "harrietj@inprice.com";

        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail(email);

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));

        given()
            .body(user).
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo(email + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_9() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail("jd@in.io");

        given()
            .body(user).
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail(StringUtils.repeat('a', 251));

        given()
            .body(user).
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_is_invalid() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail("test@invalid");

        given()
            .body(user).
        when()
            .put(Consts.Paths.User.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void everything_should_be_ok_with_changing_password() {
        final PasswordDTO pass = new PasswordDTO(1L);
        pass.setPasswordOld("p4ssw0rd");
        pass.setPassword("p4ssw0rd-new");
        pass.setPasswordAgain("p4ssw0rd-new");

        given()
            .body(pass).
        when()
            .put(Consts.Paths.User.PASSWORD).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void password_cannot_be_null() {
        final PasswordDTO pass = new PasswordDTO(1L);

        given()
            .body(pass).
        when()
            .put(Consts.Paths.User.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

    @Test
    public void password_length_is_out_of_range_if_less_than_4() {
        final PasswordDTO pass = new PasswordDTO(1L);
        pass.setPassword("pas");

        given()
            .body(pass).
        when()
            .put(Consts.Paths.User.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason", hasItem("Password length must be between 4 and 16 chars!"));
    }

    @Test
    public void password_length_is_out_of_range_if_greater_than_16() {
        final PasswordDTO pass = new PasswordDTO(1L);
        pass.setPassword(StringUtils.repeat('a', 17));

        given()
            .body(pass).
        when()
            .put(Consts.Paths.User.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Password length must be between 4 and 16 chars!"));
    }

    @Test
    public void password_are_mismatch() {
        final PasswordDTO pass = new PasswordDTO(1L);
        pass.setPassword("password");
        pass.setPasswordAgain("p4ssw0rd");

        given()
            .body(pass).
        when()
            .put(Consts.Paths.User.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Passwords are mismatch!"));
    }

    @Test
    public void old_password_cannot_be_null() {
        final PasswordDTO pass = new PasswordDTO(1L);
        pass.setPassword("p4ssw0rd");
        pass.setPasswordAgain("p4ssw0rd");

        given()
            .body(pass).
        when()
            .put(Consts.Paths.User.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Old password cannot be null!"));
    }

    @Test
    public void old_password_is_incorrect() {
        final PasswordDTO pass = new PasswordDTO(1L);
        pass.setPasswordOld("wrong");
        pass.setPassword("p4ssw0rd");
        pass.setPasswordAgain("p4ssw0rd");

        given()
            .body(pass).
        when()
            .put(Consts.Paths.User.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Old password is incorrect!"));
    }

}
