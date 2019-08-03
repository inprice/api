package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Config;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Global;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class CompanyTest {

    private static final String ROOT = "/company";

    private final Config config = Beans.getSingleton(Config.class);
    private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    @BeforeClass
    public static void setup() {
        if (Global.isApplicationRunning) {
            dbUtils.reset();
        } else {
            Application.main(null);
        }
    }

    @Test
    public void everything_should_be_ok_with_insert() {
        final CompanyDTO company = createAValidCompany();
        company.setEmail("test@test.com");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void invalid_data_for_company() {
        given()
            .port(config.getAPP_Port())
            .body("wrong body!").
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Invalid data for company!"));
    }

    @Test
    public void company_name_cannot_be_null() {
        final CompanyDTO company = createAValidCompany();
        company.setCompanyName(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Company name cannot be null!"));
    }

    @Test
    public void company_name_length_is_out_of_range_if_less_than_3() {
        final CompanyDTO company = createAValidCompany();
        company.setCompanyName("AA");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("The length of name field must be between 3 and 250 chars!"));
    }

    @Test
    public void company_name_length_is_out_of_range_if_greater_than_250() {
        final CompanyDTO company = createAValidCompany();
        company.setCompanyName(StringUtils.repeat('A', 251));

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("The length of name field must be between 3 and 250 chars!"));
    }

    @Test
    public void company_not_found_for_a_wrong_id_when_updated() {
        final CompanyDTO company = createAValidCompany();
        company.setId(0L);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Company not found!"));
    }

    @Test
    public void you_should_pick_a_country_when_country_not_presented() {
        final CompanyDTO company = createAValidCompany();
        company.setCountryId(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("You should pick a country!"));
    }

    @Test
    public void unknown_country_for_a_wrong_id() {
        final CompanyDTO company = createAValidCompany();
        company.setCountryId(9999L);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Unknown country!"));
    }

    @Test
    public void contact_name_cannot_be_null() {
        final CompanyDTO company = createAValidCompany();
        company.setFullName(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Contact name cannot be null!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_less_than_2() {
        final CompanyDTO company = createAValidCompany();
        company.setFullName("A");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Contact name must be between 2 and 150 chars!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_greater_than_150() {
        final CompanyDTO company = createAValidCompany();
        company.setFullName(StringUtils.repeat('A', 151));

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Contact name must be between 2 and 150 chars!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final CompanyDTO company = createAValidCompany();
        company.setEmail(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final String email = "mustafa@inprice.com";

        final CompanyDTO company = createAValidCompany();
        company.setEmail(email);

        given()
            .port(config.getAPP_Port())
            .body(company)
        .post(ROOT);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo(email + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_9() {
        final CompanyDTO company = createAValidCompany();
        company.setEmail("jd@in.io");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final CompanyDTO company = createAValidCompany();
        company.setEmail(StringUtils.repeat('a', 251));

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_is_invalid() {
        final CompanyDTO company = createAValidCompany();
        company.setEmail("test@invalid");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void password_cannot_be_null() {
        final CompanyDTO company = createAValidCompany();
        company.setPassword(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

    @Test
    public void password_length_is_out_of_range_if_less_than_5() {
        final CompanyDTO company = createAValidCompany();
        company.setPassword("pass");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_length_is_out_of_range_if_greater_than_16() {
        final CompanyDTO company = createAValidCompany();
        company.setPassword(StringUtils.repeat('a', 17));

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_are_mismatch() {
        final CompanyDTO company = createAValidCompany();
        company.setPasswordAgain("password"); // --> password is p4ssw0rd

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Passwords are mismatch!"));
    }

    private CompanyDTO createAValidCompany() {
        CompanyDTO company = new CompanyDTO();
        company.setCompanyName("inprice");
        company.setWebsite("www.inprice.io");
        company.setFullName("John Doe");
        company.setEmail("jdoe@inprice.io");
        company.setPassword("p4ssw0rd");
        company.setPasswordAgain("p4ssw0rd");
        company.setCountryId(1L);
        return company;
    }

}
