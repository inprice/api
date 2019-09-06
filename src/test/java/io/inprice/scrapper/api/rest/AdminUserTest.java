package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.TestHelper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class AdminUserTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true);

        //insert an ordinary user to manipulate him
        final UserDTO user = TestHelper.getUserDTO();
        user.setId(3L);
        user.setEmail("ordinary@inprice.io");

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

    }

    @Test
    public void everything_should_be_ok_with_inserting() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail("test@test.com");

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_finding() {
        final int id = 1;

        when()
            .get(Consts.Paths.AdminUser.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("model.id", equalTo(id));
    }

    @Test
    public void user_not_found_when_get_with_a_wrong_id() {
        when()
            .get(Consts.Paths.AdminUser.BASE + "/0").
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body("result", equalTo("Invalid user!"));
    }

    @Test
    public void fail_to_delete_admin_user() {
        final int id = 1;

        when()
            .delete(Consts.Paths.AdminUser.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void user_not_found_when_delete_with_a_wrong_id() {
        when()
            .delete(Consts.Paths.AdminUser.BASE + "/0").
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body("result", equalTo("Invalid user!"));
    }

    @Test
    public void everything_should_be_ok_with_deleting() {
        final long id = 4;

        final UserDTO user = TestHelper.getUserDTO();
        user.setId(id);
        user.setEmail("harrietj@inprice.io");

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

        when()
            .delete(Consts.Paths.AdminUser.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_listing() {
        when()
            .get(Consts.Paths.AdminUser.BASE + "s").
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("models.size", greaterThan(0)); //since we have a default user inserted at the beginning
    }

    @Test
    public void everything_should_be_ok_with_toggling_status() {
        final int userId = 3;

        //should return true
        when()
            .get(Consts.Paths.AdminUser.BASE + "/" + userId).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
        .body("model.active", equalTo(true));

        //should set false
        when()
            .put(Consts.Paths.AdminUser.TOGGLE_STATUS + "/" + userId).
        then()
            .statusCode(HttpStatus.OK_200).assertThat(); //since we have a default user inserted at the beginning

        //should return false
        when()
            .get(Consts.Paths.AdminUser.BASE + "/" + userId).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
        .body("model.active", equalTo(false));
    }

    @Test
    public void fail_to_toggle_admins_status() {
        final int userId = 1;

        //should set false
        when()
            .put(Consts.Paths.AdminUser.TOGGLE_STATUS + "/" + userId).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void everything_should_be_ok_with_updating() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setFullName("Jane Doe");
        user.setEmail("janed@inprice.io");

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void invalid_data_for_user() {
        given()
            .body("wrong body!").
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body("result", equalTo("Invalid user!"));
    }

    @Test
    public void full_name_cannot_be_null() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setFullName(null);

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Full name cannot be null!"));
    }

    @Test
    public void full_name_length_is_out_of_range_if_less_than_3() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setFullName("A");

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Full name must be between 2 and 150 chars!"));
    }

    @Test
    public void full_name_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setFullName(StringUtils.repeat('A', 251));

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Full name must be between 2 and 150 chars!"));
    }

    @Test
    public void user_not_found_when_update_with_a_wrong_id() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setId(0L);
        user.setEmail("test@iprice.io");

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("User not found!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail(null);

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
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
            .statusCode(HttpStatus.OK_200).assertThat();

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo(email + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_9() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail("jd@in.io");

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail(StringUtils.repeat('a', 251));

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_is_invalid() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail("test@invalid");

        given()
            .body(user).
        when()
            .post(Consts.Paths.AdminUser.BASE).
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
            .body(pass).
        when()
            .put(Consts.Paths.AdminUser.PASSWORD).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();
    }

    @Test
    public void password_cannot_be_null() {
        final PasswordDTO pass = new PasswordDTO(1L);

        given()
            .body(pass).
        when()
            .put(Consts.Paths.AdminUser.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
        .body("problems.reason[0]", equalTo("Password cannot be null!"));
    }

    @Test
    public void password_length_is_out_of_range_if_less_than_5() {
        final PasswordDTO pass = new PasswordDTO(1L);
        pass.setPassword("pass");

        given()
            .body(pass).
        when()
            .put(Consts.Paths.AdminUser.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_length_is_out_of_range_if_greater_than_16() {
        final PasswordDTO pass = new PasswordDTO(1L);
        pass.setPassword(StringUtils.repeat('a', 17));

        given()
            .body(pass).
        when()
            .put(Consts.Paths.AdminUser.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Password length must be between 5 and 16 chars!"));
    }

    @Test
    public void password_are_mismatch() {
        final PasswordDTO pass = new PasswordDTO(1L);
        pass.setPassword("password");
        pass.setPasswordAgain("p4ssw0rd");

        given()
            .body(pass).
        when()
            .put(Consts.Paths.AdminUser.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
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
            .put(Consts.Paths.AdminUser.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
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
            .put(Consts.Paths.AdminUser.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Old password is incorrect!"));
    }

}
