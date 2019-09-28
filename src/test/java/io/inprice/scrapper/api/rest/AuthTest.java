package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.dto.EmailDTO;
import io.inprice.scrapper.api.dto.LoginDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.helpers.TestHelper;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class AuthTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true);
    }

    @Test
    public void everything_should_be_ok_with_admin_login() {
        final LoginDTO login = TestHelper.getLoginDTO();

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void everything_should_be_ok_with_user_login() {
        final LoginDTO login = new LoginDTO();
        login.setEmail("jdoe@inprice.io");
        login.setPassword("p4ssw0rd");

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void invalid_email_or_password_for_login() {
        given()
            .body("wrong body!").
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.Invalid.EMAIL_OR_PASSWORD.getStatus()));
    }

    @Test
    public void email_address_cannot_be_null() {
        final LoginDTO login = TestHelper.getLoginDTO();
        login.setEmail(null);

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_9() {
        final LoginDTO login = TestHelper.getLoginDTO();
        login.setEmail("jd@in.io");

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final LoginDTO login = TestHelper.getLoginDTO();
        login.setEmail(StringUtils.repeat('a', 251));

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_is_invalid() {
        final LoginDTO login = TestHelper.getLoginDTO();
        login.setEmail("test@invalid");

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void password_cannot_be_null() {
        final LoginDTO login = TestHelper.getLoginDTO();
        login.setPassword(null);

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

    @Test
    public void password_length_is_out_of_range_if_less_than_4() {
        final LoginDTO login = TestHelper.getLoginDTO();
        login.setPassword("pas");

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason", hasItem("Password length must be between 4 and 16 chars!"));
    }

    @Test
    public void password_length_is_out_of_range_if_greater_than_16() {
        final LoginDTO login = TestHelper.getLoginDTO();
        login.setPassword(StringUtils.repeat('a', 17));

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Password length must be between 4 and 16 chars!"));
    }

    @Test
    public void everything_should_be_ok_with_refreshing() {
        RestAssured.requestSpecification = null;

        LoginDTO login = new LoginDTO();
        login.setEmail("jdoe@inprice.io");
        login.setPassword("p4ssw0rd");

        Response res =
            given()
                .body(login).
            when()
                .post(Consts.Paths.Auth.LOGIN).
            then()
                .statusCode(HttpStatus.OK_200).assertThat()
                .body("status", equalTo(Responses.OK.getStatus()))
                .extract().
        response();

        given()
          .header(Consts.Auth.AUTHORIZATION_HEADER, res.header(Consts.Auth.AUTHORIZATION_HEADER)).
        when()
            .get(Consts.Paths.Auth.REFRESH_TOKEN).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));

        TestHelper.loginAsAdmin();
    }

    @Test
    public void missing_header_when_token_is_being_refreshed_without_previous_token() {
        RestAssured.requestSpecification = null;

        when()
            .post(Consts.Paths.Auth.REFRESH_TOKEN).
        then()
            .statusCode(HttpStatus.UNAUTHORIZED_401).assertThat();

        TestHelper.loginAsAdmin();
    }

    @Test
    public void invalid_token_when_refreshing_token_for_a_logged_out_user() {
        TestHelper.logout();

        when()
            .post(Consts.Paths.Auth.REFRESH_TOKEN).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat();

        TestHelper.loginAsAdmin();
    }

    /**
     * This test case is too timely! Please use it if only if really you need!
     */
    /*
    @Test
    public void token_should_be_expired() {
        //give some time to make actual token expired
        try {
            Thread.sleep(15 * 1000L);
        } catch (InterruptedException e) {
            //
        }

        when()
            .post(Consts.Paths.Auth.REFRESH_TOKEN).
        then()
            .statusCode(HttpStatus.REQUEST_TIMEOUT_408).assertThat();

        TestHelper.loginAsAdmin();
    }
    */

    @Test
    public void everything_should_be_ok_with_forgot_and_reset_password() {
        EmailDTO email = new EmailDTO();
        email.setEmail(TestHelper.getUserDTO().getEmail());

        //forgot password request
        Response res =
            given()
                .body(email).
            when()
                .post(Consts.Paths.Auth.FORGOT_PASSWORD).
            then()
                .statusCode(HttpStatus.OK_200).assertThat()
                .body("status", equalTo(Responses.OK.getStatus()))
                .extract().
            response();

        Map resMap = Global.gson.fromJson(res.asString(), Map.class);
        final String token = resMap.get("data").toString();

        PasswordDTO password = new PasswordDTO();
        password.setToken(token);
        password.setPassword("p4ssw0rd");
        password.setPasswordAgain("p4ssw0rd");

        //reset password request
        given()
            .body(password).
        when()
            .post(Consts.Paths.Auth.RESET_PASSWORD).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));

        //loginAsAdmin with new credentials
        LoginDTO login = new LoginDTO();
        login.setEmail(email.getEmail());
        login.setPassword(password.getPassword());

        given()
            .body(login).
        when()
            .post(Consts.Paths.Auth.LOGIN).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void invalid_email_for_forgot_password() {
        given()
            .body("wrong body").
        when()
            .post(Consts.Paths.Auth.FORGOT_PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.Invalid.PASSWORD.getStatus()));
    }

    @Test
    public void email_cannot_be_null_for_forgot_password() {
        EmailDTO email = new EmailDTO();

        given()
            .body(email).
        when()
            .post(Consts.Paths.Auth.FORGOT_PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_not_found_for_forgot_password() {
        EmailDTO email = new EmailDTO();
        email.setEmail("test@inprice.io");

        given()
            .body(email).
        when()
            .post(Consts.Paths.Auth.FORGOT_PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.NotFound.EMAIL.getStatus()));
    }

    @Test
    public void passwords_are_mismatch_for_reset_password() {
        PasswordDTO password = new PasswordDTO();
        password.setPassword("p4ssw0rd");

        given()
            .body(password).
        when()
            .post(Consts.Paths.Auth.RESET_PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Passwords are mismatch!"));
    }

    @Test
    public void token_cannot_be_null_for_reset_password() {
        PasswordDTO password = new PasswordDTO();
        password.setPassword("p4ssw0rd");
        password.setPasswordAgain("p4ssw0rd");

        given()
            .body(password).
        when()
            .post(Consts.Paths.Auth.RESET_PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Token cannot be null!"));
    }

    @Test
    public void password_cannot_be_null_reset_password() {
        EmailDTO email = new EmailDTO();
        email.setEmail(TestHelper.getLoginDTO().getEmail());

        given()
            .body(email).
        when()
            .post(Consts.Paths.Auth.RESET_PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

}
