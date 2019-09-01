package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.TestHelper;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

public class ProductCSVImportTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true, true);
    }

    @Test
    public void everything_should_be_ok_with_importing_csv() {
        given()
            .body(TestHelper.loadFileFromResources("csv/correct-products.csv")).
        when()
            .post(Consts.Paths.Product.IMPORT_CSV).
        then()
            .body("result", equalTo("CSV file has been successfully uploaded.")).assertThat()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("totalCount", equalTo(2)).assertThat()
            .body("insertCount", equalTo(2)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problemCount", equalTo(0)).assertThat();
    }

    @Test
    public void everything_should_be_ok_with_importing_csv_with_duplicate_codes() {
        given()
            .body(TestHelper.loadFileFromResources("csv/duplicate-products.csv")).
        when()
            .post(Consts.Paths.Product.IMPORT_CSV).
        then()
            .body("result", equalTo("CSV file has been successfully uploaded.")).assertThat()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("totalCount", equalTo(3)).assertThat()
            .body("insertCount", equalTo(2)).assertThat()
            .body("duplicateCount", equalTo(1)).assertThat()
            .body("problemCount", equalTo(0)).assertThat();
    }

    @Test
    public void validation_problems_with_importing_csv() {
        given()
            .body(TestHelper.loadFileFromResources("csv/incorrect-products.csv")).
        when()
            .post(Consts.Paths.Product.IMPORT_CSV).
        then()
            .body("result", equalTo("Failed to import CSV file, please see details!")).assertThat()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat();
    }

    @Test
    public void format_error_with_importing_csv() {
        given()
            .body(TestHelper.loadFileFromResources("csv/format-error-products.csv")).
        when()
            .post(Consts.Paths.Product.IMPORT_CSV).
        then()
            .body("result", equalTo("Failed to import CSV file, please see details!")).assertThat()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat();
    }

    @Test
    public void validation_problems_with_importing_an_empty_csv() {
        when()
            .post(Consts.Paths.Product.IMPORT_CSV).
        then()
            .body("result", equalTo("CSV file is empty!"))
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat();
    }

    @Test
    public void field_validation_problems_with_importing_half_problematic_csv() {
        given()
            .body(TestHelper.loadFileFromResources("csv/half-correct-products.csv")).
        when()
            .post(Consts.Paths.Product.IMPORT_CSV).
        then()
            .body("result", equalTo("CSV file has been uploaded. However, some problems occurred. Please see details.")).assertThat()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("totalCount", equalTo(3)).assertThat()
            .body("insertCount", equalTo(1)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problemCount", equalTo(2)).assertThat()
            .body("problemList[0]", equalTo("002: Price must be greater than zero!")).assertThat()
            .body("problemList[1]", equalTo("003: Product name must be between 3 and 500 chars!")).assertThat();
    }

    @Test
    public void missing_column_problems_with_importing_csv() {
        given()
            .body(TestHelper.loadFileFromResources("csv/missing-column-products.csv")).
        when()
            .post(Consts.Paths.Product.IMPORT_CSV).
        then()
            .body("result", equalTo("Failed to import CSV file, please see details!")).assertThat()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("totalCount", equalTo(2)).assertThat()
            .body("insertCount", equalTo(0)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problemCount", equalTo(2)).assertThat()
            .body("problemList[0]", equalTo("001: There must be 5 columns in each row!. Column separator is comma ,")).assertThat()
            .body("problemList[1]", equalTo("002: There must be 5 columns in each row!. Column separator is comma ,")).assertThat();
    }

    @Test
    public void limit_problem_for_two_cases_with_importing_csv() {
        //case 1
        given()
            .body(TestHelper.loadFileFromResources("csv/too-many-products.csv")).
        when()
            .post(Consts.Paths.Product.IMPORT_CSV).
        then()
            .body("result", equalTo("CSV file has been uploaded. However, some problems occurred. Please see details.")).assertThat()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("totalCount", equalTo(31)).assertThat()
            .body("problemCount", equalTo(1)).assertThat()
            .body("problemList[0]", equalTo("031: You have reached your plan's maximum product limit.")).assertThat();

        //case 2
        given()
            .body(TestHelper.loadFileFromResources("csv/correct-products.csv")).
        when()
            .post(Consts.Paths.Product.IMPORT_CSV).
        then()
            .body("result", equalTo("You have already reached your plan's maximum product limit.")).assertThat()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS_429).assertThat();
    }

    @After
    public void clearAllProducts() {
        for (int i = 0; i < 50; i++) {
            delete(Consts.Paths.Product.BASE + "/" + (i+1));
        }
    }

}
