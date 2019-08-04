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
import static org.hamcrest.Matchers.greaterThan;

public class AdminUserTest {

    private static final String ROOT = "/admin/user";

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

        //insert a user to use him
        given()
            .port(config.getAPP_Port())
            .body(createAValidUser()).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

        //insert an ordinary user to manipulate him
        final UserDTO user = createAValidUser();
        user.setId(3L);
        user.setEmail("ordinary@inprice.io");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

    }

    @Test
    public void everything_should_be_ok_with_inserting() {
        final UserDTO user = createAValidUser();
        user.setEmail("test@test.com");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_finding() {
        final int id = 1;

        given()
            .port(config.getAPP_Port()).
        when()
            .get(ROOT + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("model.id", equalTo(id));
    }

    @Test
    public void user_not_found_when_get_with_a_wrong_id() {
        given()
            .port(config.getAPP_Port()).
        when()
            .get(ROOT + "/0").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void fail_to_delete_admin_user() {
        final int id = 1;

        given()
            .port(config.getAPP_Port()).
        when()
            .delete(ROOT + "/" + id).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void user_not_found_when_delete_with_a_wrong_id() {
        given()
            .port(config.getAPP_Port()).
        when()
            .delete(ROOT + "/0").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void everything_should_be_ok_with_deleting() {
        final long id = 4;

        final UserDTO user = createAValidUser();
        user.setId(id);
        user.setEmail("harrietj@inprice.io");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

        given()
            .port(config.getAPP_Port()).
        when()
            .delete(ROOT + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_listing() {
        given()
            .port(config.getAPP_Port()).
        when()
            .get(ROOT + "s").
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("models.size", greaterThan(0)); //since we have a default user inserted at the beginning
    }

    @Test
    public void everything_should_be_ok_with_toggling_status() {
        final int userId = 3;

        //should return true
        given()
            .port(config.getAPP_Port()).
        when()
            .get(ROOT + "/" + userId).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
        .body("model.active", equalTo(true));

        //should set false
        given()
            .port(config.getAPP_Port()).
        when()
            .put(ROOT + "/toggle-status/" + userId).
        then()
            .statusCode(HttpStatus.OK_200).assertThat(); //since we have a default user inserted at the beginning

        //should return false
        given()
            .port(config.getAPP_Port()).
        when()
            .get(ROOT + "/" + userId).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
        .body("model.active", equalTo(false));
    }

    @Test
    public void fail_to_toggle_admins_status() {
        final int userId = 1;

        //should set false
        given()
            .port(config.getAPP_Port()).
        when()
            .put(ROOT + "/toggle-status/" + userId).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void everything_should_be_ok_with_updating() {
        final UserDTO user = createAValidUser();
        user.setFullName("Jane Doe");
        user.setEmail("janed@inprice.io");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT).
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
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Invalid data for user!"));
    }

    @Test
    public void full_name_cannot_be_null() {
        final UserDTO user = createAValidUser();
        user.setFullName(null);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post(ROOT).
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
            .post(ROOT).
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
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Full name must be between 2 and 150 chars!"));
    }

    @Test
    public void user_not_found_when_update_with_a_wrong_id() {
        final UserDTO user = createAValidUser();
        user.setId(0L);
        user.setEmail("test@iprice.io");

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final UserDTO user = createAValidUser();
        user.setEmail(null);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final String email = "harrietj@inprice.com";

        final UserDTO user = createAValidUser();
        user.setEmail(email);

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

        given()
            .port(config.getAPP_Port())
            .body(user).
        when()
            .post(ROOT).
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
            .post(ROOT).
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
            .post(ROOT).
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
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Invalid email address!"));
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
