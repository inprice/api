package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Config;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.common.meta.UserType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class AdminTest {

    private static final String ROOT = "/admin";

    private static final Config config = Beans.getSingleton(Config.class);
    private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    @BeforeClass
    public static void setup() {
        if (Global.isApplicationRunning) {
            dbUtils.reset();
        } else {
            Application.main(null);
        }

        //insert a default company
        given()
            .port(config.getAPP_Port())
            .body(createAValidCompany()).
        when()
            .post("/company").
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

        //be careful, the id 1 is reserved for the admin only during testing

        //insert a default user
        given()
            .port(config.getAPP_Port())
            .body(createAValidUser()).
        when()
            .post(ROOT + "/user").
        then()
            .statusCode(HttpStatus.OK_200).assertThat();
    }

    @Test
    public void fail_to_delete_admin_user() {
        final int id = 1;

        given()
            .port(config.getAPP_Port()).
        when()
            .delete(ROOT + "/user/" + id).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void email_address_is_invalid() {
        final UserDTO user = createAValidUser();
        user.setEmail("test@invalid");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final UserDTO user = createAValidUser();
        user.setEmail(null);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT + "/user").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final UserDTO user = createAValidUser();
        user.setId(1L);
        user.setEmail("jdoe@inprice.io");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo(user.getEmail() + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_4() {
        final UserDTO user = createAValidUser();
        user.setEmail("jd@in.io");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 4 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = createAValidUser();
        user.setEmail(StringUtils.repeat('a', 251));

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 4 and 250 chars!"));
    }
    @Test
    public void full_name_cannot_be_null() {
        final UserDTO user = createAValidUser();
        user.setFullName(null);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Full name cannot be null!"));
    }

    @Test
    public void full_name_length_is_out_of_range_if_less_than_3() {
        final UserDTO user = createAValidUser();
        user.setFullName("A");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Full name must be between 2 and 150 chars!"));
    }

    @Test
    public void full_name_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = createAValidUser();
        user.setFullName(StringUtils.repeat('A', 251));

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Full name must be between 2 and 150 chars!"));
    }

    @Test
    public void everything_should_be_ok_with_changing_password() {
        final PasswordDTO pass = new PasswordDTO();
        pass.setId(1L);
        pass.setPasswordOld("p4ssw0rd");
        pass.setPassword("p4ssw0rd-new");
        pass.setPasswordAgain("p4ssw0rd-new");

        given()
            .port(config.getAPP_Port())
            .body(pass).
        when()
            .put(ROOT + "/password").
        then()
            .statusCode(HttpStatus.OK_200).assertThat();
    }

    @Test
    public void password_cannot_be_null() {
        final PasswordDTO pass = new PasswordDTO();

        given()
            .port(config.getAPP_Port())
            .body(pass).
        when()
            .put(ROOT + "/password").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
        .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

    @Test
    public void password_length_is_out_of_range_if_less_than_5() {
        final PasswordDTO pass = new PasswordDTO();
        pass.setPassword("pass");

        given()
            .port(config.getAPP_Port())
            .body(pass).
        when()
            .put(ROOT + "/password").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_length_is_out_of_range_if_greater_than_16() {
        final PasswordDTO pass = new PasswordDTO();
        pass.setPassword(StringUtils.repeat('a', 17));

        given()
            .port(config.getAPP_Port())
            .body(pass).
        when()
            .put(ROOT + "/password").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_are_mismatch() {
        final PasswordDTO pass = new PasswordDTO();
        pass.setPassword("password");
        pass.setPasswordAgain("p4ssw0rd");

        given()
            .port(config.getAPP_Port())
            .body(pass).
        when()
            .put(ROOT + "/password").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Passwords are mismatch!"));
    }

    @Test
    public void old_password_cannot_be_null() {
        final PasswordDTO pass = new PasswordDTO();
        pass.setPassword("p4ssw0rd");
        pass.setPasswordAgain("p4ssw0rd");

        given()
            .port(config.getAPP_Port())
            .body(pass).
        when()
            .put(ROOT + "/password").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Old password cannot be null!"));
    }

    @Test
    public void old_password_is_incorrect() {
        final PasswordDTO pass = new PasswordDTO();
        pass.setId(1L);
        pass.setPasswordOld("wrong");
        pass.setPassword("p4ssw0rd");
        pass.setPasswordAgain("p4ssw0rd");

        given()
            .port(config.getAPP_Port())
            .body(pass).
        when()
            .put(ROOT + "/password").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Old password is incorrect!"));
    }

    private static UserDTO createAValidUser() {
        UserDTO user = new UserDTO();
        user.setId(2L);
        user.setType(UserType.USER);
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
