package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.PasswordDTO;
import io.inprice.scrapper.api.dto.UserDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.DTOHelper;
import io.inprice.scrapper.api.helpers.Global;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

public class AdminTest {

    private static final Properties properties = Beans.getSingleton(Properties.class);
    private static final DBUtils dbUtils = Beans.getSingleton(DBUtils.class);

    @BeforeClass
    public static void setup() {
        if (Global.isApplicationRunning) {
            dbUtils.reset();
        } else {
            Application.main(null);
        }

        RestAssured.port = properties.getAPP_Port();

        //insert a default company
        given()
            .body(DTOHelper.getCompanyDTO()).
        when()
            .post(Consts.Paths.Company.REGISTER).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

        //be careful, the id 1 is reserved for the admin only during testing
        Response res =
            given()
                .body(DTOHelper.getLoginDTO()).
            when()
                .post(Consts.Paths.Auth.LOGIN).
            then()
                .extract().
            response();

        RestAssured.requestSpecification =
            new RequestSpecBuilder()
                .addHeader(Consts.Auth.AUTHORIZATION_HEADER, res.header(Consts.Auth.AUTHORIZATION_HEADER))
            .build();

        //insert a default user
        given()
            .body(DTOHelper.getUserDTO()).
        when()
            .post(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();
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
    public void email_address_is_invalid() {
        final UserDTO user = DTOHelper.getUserDTO();
        user.setEmail("test@invalid");

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Invalid email address!"));
    }

    @Test
    public void email_address_cannot_be_null() {
        final UserDTO user = DTOHelper.getUserDTO();
        user.setEmail(null);

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address cannot be null!"));
    }

    @Test
    public void email_address_is_already_used_by_another_user() {
        final UserDTO user = DTOHelper.getUserDTO();
        user.setId(1L);
        user.setEmail("jdoe@inprice.io");

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo(user.getEmail() + " is already used by another user!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_less_than_4() {
        final UserDTO user = DTOHelper.getUserDTO();
        user.setEmail("jd@in.io");

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }

    @Test
    public void email_address_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = DTOHelper.getUserDTO();
        user.setEmail(StringUtils.repeat('a', 251));

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Email address must be between 9 and 250 chars!"));
    }
    @Test
    public void full_name_cannot_be_null() {
        final UserDTO user = DTOHelper.getUserDTO();
        user.setFullName(null);

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Full name cannot be null!"));
    }

    @Test
    public void full_name_length_is_out_of_range_if_less_than_3() {
        final UserDTO user = DTOHelper.getUserDTO();
        user.setFullName("A");

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Full name must be between 2 and 150 chars!"));
    }

    @Test
    public void full_name_length_is_out_of_range_if_greater_than_250() {
        final UserDTO user = DTOHelper.getUserDTO();
        user.setFullName(StringUtils.repeat('A', 251));

        given()
            .body(user).
        when()
            .put(Consts.Paths.AdminUser.BASE).
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
            .put(Consts.Paths.AdminUser.PASSWORD).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();
    }

    @Test
    public void password_cannot_be_null() {
        final PasswordDTO pass = new PasswordDTO();

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
        final PasswordDTO pass = new PasswordDTO();
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
        final PasswordDTO pass = new PasswordDTO();
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
        final PasswordDTO pass = new PasswordDTO();
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
        final PasswordDTO pass = new PasswordDTO();
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
        final PasswordDTO pass = new PasswordDTO();
        pass.setId(1L);
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
