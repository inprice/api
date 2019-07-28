package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Config;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.framework.Beans;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@RunWith(JUnit4.class)
public class CompanyTest {

    private final Config config = Beans.getSingleton(Config.class);

    @BeforeClass
    public static void setup() {
        Application.main(null);
    }

    @AfterClass
    public static void tearDown() {
        Application.shutdown();
    }

    @Test
    public void invalid_data_for_company() {
        given()
            .port(config.getAPP_Port())
            .body("wrong body!").
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Invalid data for company!"));
    }

    @Test
    public void company_name_cannot_be_null() {
        final CompanyDTO company = createACompanyDTO();
        company.setName(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Company name cannot be null!"));
    }

    @Test
    public void company_name_length_is_out_of_range_if_less_than_3() {
        final CompanyDTO company = createACompanyDTO();
        company.setName("AA");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("The length of name field must be between 3 and 250 chars!"));
    }

    @Test
    public void company_name_length_is_out_of_range_if_greater_than_250() {
        final CompanyDTO company = createACompanyDTO();
        company.setName(StringUtils.repeat('A', 251));

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("The length of name field must be between 3 and 250 chars!"));
    }

    @Test
    public void company_not_found_for_a_wrong_id_when_updated() {
        final CompanyDTO company = createACompanyDTO();
        company.setId(0L);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .put("/company").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Company not found!"));
    }

    @Test
    public void you_should_pick_a_country_when_country_not_presented() {
        final CompanyDTO company = createACompanyDTO();
        company.setCountryId(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("You should pick a country!"));
    }

    @Test
    public void unknown_country_for_a_wrong_id() {
        final CompanyDTO company = createACompanyDTO();
        company.setCountryId(9999L);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Unknown country!"));
    }

    @Test
    public void contact_name_cannot_be_null() {
        final CompanyDTO company = createACompanyDTO();
        company.setContactName(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Contact name cannot be null!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_less_than_2() {
        final CompanyDTO company = createACompanyDTO();
        company.setContactName("A");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Contact name must be between 2 and 150 chars!"));
    }

    @Test
    public void contact_name_length_is_out_of_range_if_greater_than_150() {
        final CompanyDTO company = createACompanyDTO();
        company.setContactName(StringUtils.repeat('A', 151));

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Contact name must be between 2 and 150 chars!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final CompanyDTO company = createACompanyDTO();
        company.setContactEmail(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final String email = "mustafa@inprice.com";

        final CompanyDTO company = createACompanyDTO();
        company.setContactEmail(email);

        given()
            .port(config.getAPP_Port())
            .body(company)
        .post("/company");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo(email + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_9() {
        final CompanyDTO company = createACompanyDTO();
        company.setContactEmail("jd@in.io");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final CompanyDTO company = createACompanyDTO();
        company.setContactEmail(StringUtils.repeat('a', 251));

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_is_invalid() {
        final CompanyDTO company = createACompanyDTO();
        company.setContactEmail("test@invalid");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void password_cannot_be_null() {
        final CompanyDTO company = createACompanyDTO();
        company.setPassword(null);

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

    @Test
    public void password_length_is_out_of_range_if_less_than_5() {
        final CompanyDTO company = createACompanyDTO();
        company.setPassword("pass");

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_length_is_out_of_range_if_greater_than_16() {
        final CompanyDTO company = createACompanyDTO();
        company.setPassword(StringUtils.repeat('a', 17));

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_are_mismatch() {
        final CompanyDTO company = createACompanyDTO();
        company.setPasswordAgain("password"); // --> password is p4ssw0rd

        given()
            .port(config.getAPP_Port())
            .body(company).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Passwords are mismatch!"));
    }

    private CompanyDTO createACompanyDTO() {
        CompanyDTO company = new CompanyDTO();
        company.setName("inprice");
        company.setWebsite("www.inprice.io");
        company.setContactName("John Doe");
        company.setContactEmail("jdoe@inprice.io");
        company.setPassword("p4ssw0rd");
        company.setPasswordAgain("p4ssw0rd");
        company.setCountryId(1L);
        return company;
    }

}
