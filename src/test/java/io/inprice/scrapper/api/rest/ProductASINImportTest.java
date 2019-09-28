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

public class ProductASINImportTest {

    private static final String PATH = Consts.Paths.Product.IMPORT_AMAZON_ASIN_LIST;
    private static final String ROOT = "asin";
    private static final String TYPE = "ASIN list";
    
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
            .body("result", equalTo(TYPE + " has been successfully uploaded.")).assertThat()
            .body("totalCount", equalTo(2)).assertThat()
            .body("insertCount", equalTo(2)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problemCount", equalTo(0)).assertThat();
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
            .body("result", equalTo(TYPE + " has been successfully uploaded.")).assertThat()
            .body("totalCount", equalTo(3)).assertThat()
            .body("insertCount", equalTo(2)).assertThat()
            .body("duplicateCount", equalTo(1)).assertThat()
            .body("problemCount", equalTo(0)).assertThat();
    }

    @Test
    public void validation_problems() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/incorrect-products.txt")).
        when()
            .post(PATH).
        then()
            .body("result", equalTo("Failed to import " + TYPE + ", please see details!")).assertThat()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat();
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
            .body("result", equalTo(TYPE + " has been uploaded. However, some problems occurred. Please see details.")).assertThat()
            .body("totalCount", equalTo(3)).assertThat()
            .body("insertCount", equalTo(1)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problemCount", equalTo(2)).assertThat()
            .body("problemList[0]", equalTo("2. Invalid ASIN code!")).assertThat()
            .body("problemList[1]", equalTo("3. Invalid ASIN code!")).assertThat();
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
            .body("result", equalTo(TYPE + " has been uploaded. However, some problems occurred. Please see details.")).assertThat()
            .body("totalCount", equalTo(31)).assertThat()
            .body("problemCount", equalTo(1)).assertThat()
            .body("problemList[0]", equalTo("31. You have reached your plan's maximum product limit.")).assertThat();

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
