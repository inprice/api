package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.helpers.Consts;
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
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void invalid_data_for_company() {
        given()
            .body("wrong body!").
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Invalid data for company!"));
    }

    @Test
    public void company_name_cannot_be_null() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setCompanyName(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
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
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("The length of name field must be between 3 and 250 chars!"));
    }

    @Test
    public void company_name_length_is_out_of_range_if_greater_than_250() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail("test@inprice.io");
        company.setCompanyName(StringUtils.repeat('A', 251));

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("The length of name field must be between 3 and 250 chars!"));
    }

    @Test
    public void user_has_no_permission_to_update_another_company() {
        final CompanyDTO company = TestHelper.getCompanyDTO();

        //insert a default company
        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

        Response res =
            given()
                .body(TestHelper.getLoginDTO()).
            when()
                .post(Consts.Paths.Auth.LOGIN).
            then()
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
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body("result", equalTo("User has no permission to update this company!"));
    }

    @Test
    public void you_should_pick_a_country_when_country_not_presented() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setCountryId(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason", hasItem("You should pick a country!"));
    }

    @Test
    public void unknown_country_for_a_wrong_id() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setCountryId(9999L);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason", hasItem("Unknown country!"));
    }

    @Test
    public void contact_name_cannot_be_null() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setFullName(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Contact name cannot be null!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_less_than_2() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setFullName("A");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Contact name must be between 2 and 150 chars!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_greater_than_150() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setFullName(StringUtils.repeat('A', 151));

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Contact name must be between 2 and 150 chars!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final String email = "harrietj@inprice.com";

        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail(email);

        given()
            .body(company)
        .post(Consts.Paths.Company.REGISTER);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo(email + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_9() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail("jd@in.io");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail(StringUtils.repeat('a', 251));

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_is_invalid() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setEmail("test@invalid");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void password_cannot_be_null() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setPassword(null);

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

    @Test
    public void password_length_is_out_of_range_if_less_than_5() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setPassword("pass");

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_length_is_out_of_range_if_greater_than_16() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setPassword(StringUtils.repeat('a', 17));

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_are_mismatch() {
        final CompanyDTO company = TestHelper.getCompanyDTO();
        company.setPasswordAgain("password"); // --> password is p4ssw0rd

        given()
            .body(company).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Passwords are mismatch!"));
    }

}
