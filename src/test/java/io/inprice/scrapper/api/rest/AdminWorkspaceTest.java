package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.Application;
import io.inprice.scrapper.api.config.Config;
import io.inprice.scrapper.api.dto.CompanyDTO;
import io.inprice.scrapper.api.dto.WorkspaceDTO;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.helpers.DBUtils;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.common.models.Workspace;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class AdminWorkspaceTest {

    private static final String ROOT = "/admin/workspace";

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

        //insert a secondary workspace
        given()
            .port(config.getAPP_Port())
            .body(createAValidWorkspace()).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();
    }

    @Test
    public void everything_should_be_ok_with_inserting() {
        final WorkspaceDTO workspace = createAValidWorkspace();

        given()
            .port(config.getAPP_Port())
            .body(workspace).
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
    public void workspace_not_found_when_get_with_a_wrong_id() {
        given()
            .port(config.getAPP_Port()).
        when()
            .get(ROOT + "/0").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Workspace not found!"));
    }

    @Test
    public void workspace_not_found_when_delete_with_a_wrong_id() {
        given()
            .port(config.getAPP_Port()).
        when()
            .delete(ROOT + "/0").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Workspace not found!"));
    }

    @Test
    public void everything_should_be_ok_with_deleting() {
        final long id = 3;

        final WorkspaceDTO workspace = createAValidWorkspace();
        workspace.setName("A THIRD WS");

        given()
            .port(config.getAPP_Port())
            .body(workspace).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));

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
            .body("models.size", greaterThan(0)); //since we have a default workspace inserted at the beginning
    }

    @Test
    public void everything_should_be_ok_with_updating() {
        final WorkspaceDTO workspace = createAValidWorkspace();
        workspace.setId(2L);
        workspace.setName("SPECIAL WS");

        given()
            .port(config.getAPP_Port())
            .body(workspace).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void invalid_data_for_workspace() {
        given()
            .port(config.getAPP_Port())
            .body("wrong body!").
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Invalid data for workspace!"));
    }

    @Test
    public void workspace_name_cannot_be_null() {
        final WorkspaceDTO workspace = createAValidWorkspace();
        workspace.setName(null);

        given()
            .port(config.getAPP_Port())
            .body(workspace).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Workspace name cannot be null!"));
    }

    @Test
    public void workspace_name_length_is_out_of_range_if_less_than_3() {
        final WorkspaceDTO workspace = createAValidWorkspace();
        workspace.setName("AA");

        given()
            .port(config.getAPP_Port())
            .body(workspace).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Workspace name must be between 3 and 50 chars!"));
    }

    @Test
    public void workspace_name_length_is_out_of_range_if_greater_than_50() {
        final WorkspaceDTO workspace = createAValidWorkspace();
        workspace.setName(StringUtils.repeat('A', 51));

        given()
            .port(config.getAPP_Port())
            .body(workspace).
        when()
            .post(ROOT).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Workspace name must be between 3 and 50 chars!"));
    }

    @Test
    public void workspace_not_found_when_update_with_a_wrong_id() {
        final WorkspaceDTO workspace = createAValidWorkspace();
        workspace.setId(0L);

        given()
            .port(config.getAPP_Port())
            .body(workspace).
        when()
            .put(ROOT).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Workspace not found!"));
    }

    private static WorkspaceDTO createAValidWorkspace() {
        WorkspaceDTO ws = new WorkspaceDTO();
        ws.setName("SECONDARY WS");
        ws.setCompanyId(1L);
        ws.setPlanId(1L);
        return ws;
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
