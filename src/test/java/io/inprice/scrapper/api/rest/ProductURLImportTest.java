package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.helpers.TestHelper;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

public class ProductURLImportTest {

    private static final String PATH = Consts.Paths.Product.IMPORT_URL_LIST;
    private static final String ROOT = "url";
    private static final String TYPE = "URL list";
    
    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true, true);
    }

    @Test
    public void everything_should_be_ok() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/correct-products.txt")).
        when()
            .post(PATH).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()))
            .body("result", equalTo(TYPE + " has been successfully uploaded."))
            .body("totalCount", equalTo(2))
            .body("insertCount", equalTo(2))
            .body("duplicateCount", equalTo(0))
            .body("problemCount", equalTo(0));
    }

    @Test
    public void everything_should_be_ok_with_duplicate_codes() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/duplicate-products.txt")).
        when()
            .post(PATH).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()))
            .body("result", equalTo(TYPE + " has been successfully uploaded."))
            .body("totalCount", equalTo(3))
            .body("insertCount", equalTo(2))
            .body("duplicateCount", equalTo(1))
            .body("problemCount", equalTo(0));
    }

    @Test
    public void validation_problems() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/incorrect-products.txt")).
        when()
            .post(PATH).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.DataProblem.NOT_SUITABLE.getStatus()))
            .body("result", equalTo("Failed to import " + TYPE + ", please see details!"));
    }

    @Test
    public void validation_problems_for_an_empty_file() {
        when()
            .post(PATH).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.Invalid.EMPTY_FILE.getStatus()));
    }

    @Test
    public void field_validation_problems() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/half-correct-products.txt")).
        when()
            .post(PATH).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()))
            .body("result", equalTo(TYPE + " has been uploaded. However, some problems occurred. Please see details."))
            .body("totalCount", equalTo(3))
            .body("insertCount", equalTo(2))
            .body("duplicateCount", equalTo(0))
            .body("problemCount", equalTo(1))
            .body("problemList[0]", equalTo("004: Invalid URL!"));
    }

    @Test
    public void limit_problem_for_two_cases() {
        //case 1
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/too-many-products.txt")).
        when()
            .post(PATH).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()))
            .body("result", equalTo(TYPE + " has been uploaded. However, some problems occurred. Please see details."))
            .body("totalCount", equalTo(31))
            .body("problemCount", equalTo(1))
            .body("problemList[0]", equalTo("031: You have reached your plan's maximum product limit."));

        //case 2
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/correct-products.txt")).
        when()
            .post(PATH).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("status", equalTo(Responses.ServerProblem.LIMIT_PROBLEM.getStatus()))
            .body("result", equalTo("You have already reached your plan's maximum product limit."));
    }

    @After
    public void clearAllProducts() {
        for (int i = 0; i < 5; i++) {
            delete(Consts.Paths.Product.IMPORT_BASE + "/" + (i+1));
        }
    }

}
