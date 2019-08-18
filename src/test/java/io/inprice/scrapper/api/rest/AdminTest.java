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

//todo: some test cases must be added for insert and update operations
public class AdminTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true);
    }

    @Test
    public void email_address_is_invalid() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail("test@invalid");

        given()
            .body(user).
        when()
            .put(Consts.Paths.Admin.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail(null);

        given()
            .body(user).
        when()
            .put(Consts.Paths.Admin.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setId(1L);
        user.setEmail("jdoe@inprice.io");

        given()
            .body(user).
        when()
            .put(Consts.Paths.Admin.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo(user.getEmail() + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_9() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setEmail("jd@in.io");

        given()
            .body(user).
        when()
            .put(Consts.Paths.Admin.BASE).
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
            .put(Consts.Paths.Admin.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }
    @Test
    public void full_name_cannot_be_null() {
        final UserDTO user = TestHelper.getUserDTO();
        user.setFullName(null);

        given()
            .body(user).
        when()
            .put(Consts.Paths.Admin.BASE).
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
            .put(Consts.Paths.Admin.BASE).
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
            .put(Consts.Paths.Admin.BASE).
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
            .body(pass).
        when()
            .put(Consts.Paths.Admin.PASSWORD).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();
    }

    @Test
    public void password_cannot_be_null() {
        final PasswordDTO pass = new PasswordDTO(1L);

        given()
            .body(pass).
        when()
            .put(Consts.Paths.Admin.PASSWORD).
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
            .put(Consts.Paths.Admin.PASSWORD).
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
            .put(Consts.Paths.Admin.PASSWORD).
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
            .put(Consts.Paths.Admin.PASSWORD).
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
            .put(Consts.Paths.Admin.PASSWORD).
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
            .body(pass).
        when()
            .put(Consts.Paths.Admin.PASSWORD).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Old password is incorrect!"));
    }

    @Test
    public void invalid_data_for_user() {
        given()
            .body("wrong body!").
        when()
            .put(Consts.Paths.Admin.BASE).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body("result", equalTo("Invalid data for user!"));
    }

    @Test
    public void invalid_data_for_password() {
        given()
            .body("wrong body!").
        when()
            .put(Consts.Paths.Admin.PASSWORD).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body("result", equalTo("Invalid data for password!"));
    }

    @Test
    public void workspace_not_found_for_a_wrong_ws_id() {
        when()
            .put(Consts.Paths.Admin.WORKSPACE + "/5").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Workspace not found!"));
    }

    @Test
    public void everything_should_be_ok_with_changing_workspace() {
        final WorkspaceDTO workspace = TestHelper.getWorkspaceDTO();
        workspace.setName("A SECOND WS");

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));

        when()
            .put(Consts.Paths.Admin.WORKSPACE + "/2").
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void permission_problem_for_a_user_with_changing_admins_password() {
        TestHelper.loginAsUser();

        when()
            .put(Consts.Paths.Admin.PASSWORD).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat();

        TestHelper.loginAsAdmin();
    }

}
