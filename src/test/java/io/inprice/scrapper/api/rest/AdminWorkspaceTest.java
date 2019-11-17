package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.helpers.TestHelper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class AdminWorkspaceTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, false);

        //insert a secondary workspace
        given()
            .body(TestHelper.getWorkspaceDTO()).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void everything_should_be_ok_with_inserting() {
        final WorkspaceDTO workspace = TestHelper.getWorkspaceDTO();

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void everything_should_be_ok_with_finding() {
        final int id = 1;

        when()
            .get(Consts.Paths.Workspace.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()))
            .body("model.id", equalTo(id));
    }

    @Test
    public void workspace_not_found_when_get_with_a_wrong_id() {
        when()
            .get(Consts.Paths.Workspace.BASE + "/0").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.NotFound.WORKSPACE.getStatus()));
    }

    @Test
    public void workspace_not_found_when_delete_with_a_wrong_id() {
        when()
            .delete(Consts.Paths.Workspace.BASE + "/0").
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.NotFound.WORKSPACE.getStatus()));
    }

    @Test
    public void everything_should_be_ok_with_deleting() {
        final long id = 3;

        final WorkspaceDTO workspace = TestHelper.getWorkspaceDTO();
        workspace.setName("A THIRD WS");

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));

        when()
            .delete(Consts.Paths.Workspace.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void everything_should_be_ok_with_listing() {
        when()
            .get(Consts.Paths.Workspace.BASE + "s").
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()))
            .body("models.size", greaterThan(0)); //since we have a default workspace inserted at the beginning
    }

    @Test
    public void everything_should_be_ok_with_updating() {
        final WorkspaceDTO workspace = TestHelper.getWorkspaceDTO();
        workspace.setId(2L);
        workspace.setName("SPECIAL WS");

        given()
            .body(workspace).
        when()
            .put(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void invalid_data_for_workspace() {
        given()
            .body("wrong body!").
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.Invalid.WORKSPACE.getStatus()));
    }

    @Test
    public void workspace_name_cannot_be_null() {
        final WorkspaceDTO workspace = TestHelper.getWorkspaceDTO();
        workspace.setName(null);

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Workspace name cannot be null!"));
    }

    @Test
    public void workspace_name_length_is_out_of_range_if_less_than_3() {
        final WorkspaceDTO workspace = TestHelper.getWorkspaceDTO();
        workspace.setName("AA");

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Workspace name must be between 3 and 50 chars!"));
    }

    @Test
    public void workspace_name_length_is_out_of_range_if_greater_than_50() {
        final WorkspaceDTO workspace = TestHelper.getWorkspaceDTO();
        workspace.setName(StringUtils.repeat('A', 51));

        given()
            .body(workspace).
        when()
            .post(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Workspace name must be between 3 and 50 chars!"));
    }

    @Test
    public void workspace_is_missing_when_update_with_an_unknown_id() {
        final WorkspaceDTO workspace = TestHelper.getWorkspaceDTO();
        workspace.setId(0L);

        given()
            .body(workspace).
        when()
            .put(Consts.Paths.Workspace.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.NotFound.WORKSPACE.getStatus()));
    }

    @Test
    public void master_ws_cannot_be_deleted() {
        when()
            .delete(Consts.Paths.Workspace.BASE + "/1").
        then()
            .body("status", equalTo(Responses.DataProblem.MASTER_WS_CANNOT_BE_DELETED.getStatus()))
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat();
    }

}
