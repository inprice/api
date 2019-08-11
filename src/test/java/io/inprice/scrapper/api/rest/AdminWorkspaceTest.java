package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Properties;
import io.inprice.scrapper.api.dto.WorkspaceDTO;
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
import static org.hamcrest.Matchers.greaterThan;

public class AdminWorkspaceTest {

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

        //insert a secondary workspace
        given()
            .body(DTOHelper.getWorkspaceDTO()).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();
    }

    @Test
    public void everything_should_be_ok_with_inserting() {
        final WorkspaceDTO workspace = DTOHelper.getWorkspaceDTO();

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_finding() {
        final int id = 1;

        when()
            .get(Consts.Paths.Workspace.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("model.id", equalTo(id));
    }

    @Test
    public void workspace_not_found_when_get_with_a_wrong_id() {
        when()
            .get(Consts.Paths.Workspace.BASE + "/0").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Workspace not found!"));
    }

    @Test
    public void workspace_not_found_when_delete_with_a_wrong_id() {
        when()
            .delete(Consts.Paths.Workspace.BASE + "/0").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Workspace not found!"));
    }

    @Test
    public void everything_should_be_ok_with_deleting() {
        final long id = 3;

        final WorkspaceDTO workspace = DTOHelper.getWorkspaceDTO();
        workspace.setName("A THIRD WS");

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));

        when()
            .delete(Consts.Paths.Workspace.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_listing() {
        when()
            .get(Consts.Paths.Workspace.BASE + "s").
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("models.size", greaterThan(0)); //since we have a default workspace inserted at the beginning
    }

    @Test
    public void everything_should_be_ok_with_updating() {
        final WorkspaceDTO workspace = DTOHelper.getWorkspaceDTO();
        workspace.setId(2L);
        workspace.setName("SPECIAL WS");

        given()
            .body(workspace).
        when()
            .put(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void invalid_data_for_workspace() {
        given()
            .body("wrong body!").
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Invalid data for workspace!"));
    }

    @Test
    public void workspace_name_cannot_be_null() {
        final WorkspaceDTO workspace = DTOHelper.getWorkspaceDTO();
        workspace.setName(null);

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Workspace name cannot be null!"));
    }

    @Test
    public void workspace_name_length_is_out_of_range_if_less_than_3() {
        final WorkspaceDTO workspace = DTOHelper.getWorkspaceDTO();
        workspace.setName("AA");

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Workspace name must be between 3 and 50 chars!"));
    }

    @Test
    public void workspace_name_length_is_out_of_range_if_greater_than_50() {
        final WorkspaceDTO workspace = DTOHelper.getWorkspaceDTO();
        workspace.setName(StringUtils.repeat('A', 51));

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Workspace name must be between 3 and 50 chars!"));
    }

    @Test
    public void workspace_not_found_when_update_with_a_wrong_id() {
        final WorkspaceDTO workspace = DTOHelper.getWorkspaceDTO();
        workspace.setId(0L);

        given()
            .body(workspace).
        when()
            .put(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Workspace not found!"));
    }

}
