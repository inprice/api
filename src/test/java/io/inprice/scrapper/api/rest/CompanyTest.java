package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.helpers.TestHelper;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class CompanyTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(false, false);
    }

    @Test
    public void everything_should_be_ok_with_insert() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail("test@test.com");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void everything_should_be_ok_with_update() {
        final CompanyDTO company = TestHelper.getCompanyDTO();

        //insert a default company
        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));

        Response res =
            given()
                .body(TestHelper.getLoginDTO()).
            when()
                .post(Consts.Paths.Auth.LOGIN).
            then()
                .extract().
            response();

        final String token = res.header(Consts.Auth.AUTHORIZATION_HEADER);

        company.setId(1L);
        company.setCountry("United Kingdom");

        given()
            .header(Consts.Auth.AUTHORIZATION_HEADER, token)
            .body(company).
        when()
            .put(Consts.Paths.Company.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void invalid_data_for_company() {
        given()
            .body("wrong body!").
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.Invalid.COMPANY.getStatus()));
    }

    @Test
    public void company_name_cannot_be_null() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setCompanyName(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason", hasItem("Company name cannot be null!"));
    }

    @Test
    public void company_name_length_is_out_of_range_if_less_than_3() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail("test@inprice.io");
        company.setCompanyName("AA");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Company name must be between 3 and 250 chars!"));
    }

    @Test
    public void company_name_length_is_out_of_range_if_greater_than_250() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail("test@inprice.io");
        company.setCompanyName(StringUtils.repeat('A', 251));

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Company name must be between 3 and 250 chars!"));
    }

    @Test
    public void user_has_no_permission_to_update_another_company() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail("test01@test.com");

        //insert a new company
        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));

        Response res =
            given()
                .body(TestHelper.getLoginDTO()).
            when()
                .post(Consts.Paths.Auth.LOGIN).
            then()
                .statusCode(HttpStatus.OK_200).assertThat()
                .body("status", equalTo(Responses.OK.getStatus()))
                .extract().
            response();

        final String token = res.header(Consts.Auth.AUTHORIZATION_HEADER);

        company.setId(2L);

        given()
            .header(Consts.Auth.AUTHORIZATION_HEADER, token)
            .body(company).
        when()
            .put(Consts.Paths.Company.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.PermissionProblem.UNAUTHORIZED.getStatus()));
    }

    @Test
    public void you_should_pick_a_country_when_country_not_presented() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setCountry(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason", hasItem("You should pick a country!"));
    }

    @Test
    public void you_should_pick_a_sector_when_sector_not_presented() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setSector(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason", hasItem("You should pick a sector!"));
    }

    @Test
    public void unknown_country_for_an_empty_name() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setCountry("    ");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason", hasItem("Unknown country!"));
    }

    @Test
    public void unknown_sector_for_an_empty_name() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setSector("    ");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason", hasItem("Unknown sector!"));
    }

    @Test
    public void contact_name_cannot_be_null() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setFullName(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Contact name cannot be null!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_less_than_2() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setFullName("A");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Contact name must be between 2 and 150 chars!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_greater_than_150() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setFullName(StringUtils.repeat('A', 151));

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Contact name must be between 2 and 150 chars!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final String email = "harrietj@inprice.com";

        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail(email);

        given()
            .body(company)
        .post(Consts.Paths.Auth.REGISTER);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo(email + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_9() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail("jd@in.io");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail(StringUtils.repeat('a', 251));

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_is_invalid() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail("test@invalid");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void password_cannot_be_null() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setPassword(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

    @Test
    public void password_length_is_out_of_range_if_less_than_4() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setPassword("pas");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason", hasItem("Password length must be between 4 and 16 chars!"));
    }

    @Test
    public void password_length_is_out_of_range_if_greater_than_16() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setPassword(StringUtils.repeat('a', 17));

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Password length must be between 4 and 16 chars!"));
    }

    @Test
    public void password_are_mismatch() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setPasswordAgain("password"); // --> password is p4ssw0rd

        given()
            .body(company).
        when()
            .post(Consts.Paths.Auth.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Passwords are mismatch!"));
    }

}
