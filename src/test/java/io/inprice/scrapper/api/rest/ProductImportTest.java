package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.TestHelper;
import io.inprice.scrapper.api.info.ImportProblem;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;

//todo: workspace id in headers must be tested
public class ProductImportTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true, true);
    }

    @Test
    public void everything_should_be_ok_with_importing_asin() {
        given()
            .body(TestHelper.loadFileFromResources("asin/correct-products.txt")).
        when()
            .post(Consts.Paths.Product.UPLOAD_AMAZON_ASIN_LIST).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("insertCount", equalTo(3)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problems", emptyCollectionOf(ImportProblem.class)).assertThat()
            .body("result", equalTo("Amazon ASIN list file has been successfully uploaded.")).assertThat();
    }

    @Test
    public void everything_should_be_ok_with_importing_csv() {
        given()
            .body(TestHelper.loadFileFromResources("csv/correct-products.csv")).
        when()
            .post(Consts.Paths.Product.UPLOAD_CSV).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("insertCount", equalTo(2)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problems", emptyCollectionOf(ImportProblem.class)).assertThat()
            .body("result", equalTo("CSV file has been successfully uploaded.")).assertThat();
    }

    @Test
    public void everything_should_be_ok_with_importing_csv_with_duplicate_codes() {
        given()
            .body(TestHelper.loadFileFromResources("csv/duplicate-products.csv")).
        when()
            .post(Consts.Paths.Product.UPLOAD_CSV).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("insertCount", equalTo(2)).assertThat()
            .body("duplicateCount", equalTo(1)).assertThat()
            .body("problems", emptyCollectionOf(ImportProblem.class)).assertThat()
            .body("result", equalTo("CSV file has been successfully uploaded.")).assertThat();
    }

    @Test
    public void validation_problems_with_importing_csv() {
        given()
            .body(TestHelper.loadFileFromResources("csv/incorrect-products.csv")).
        when()
            .post(Consts.Paths.Product.UPLOAD_CSV).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Failed to upload CSV file, please see details."));
    }

    @Test
    public void format_error_with_importing_csv() {
        given()
            .body(TestHelper.loadFileFromResources("csv/format-error-products.csv")).
        when()
            .post(Consts.Paths.Product.UPLOAD_CSV).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Format error! Rules: Header line isn't allowed. " +
                    "Separator must be ; and allowed Quote can be \""));
    }

    @Test
    public void validation_problems_with_importing_an_empty_csv() {
        when()
            .post(Consts.Paths.Product.UPLOAD_CSV).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("CSV file is incorrect!"));
    }

}
