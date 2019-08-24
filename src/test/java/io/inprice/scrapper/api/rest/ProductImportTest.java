package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.dto.CSVUploadDTO;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.TestHelper;
import io.inprice.scrapper.api.info.ImportProblem;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;

//todo: workspace id in headers must be tested
public class ProductImportTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true, true);
    }

    @Test
    public void everything_should_be_ok_with_importing_csv() {
        CSVUploadDTO csv = new CSVUploadDTO();
        csv.setSeparator(';');
        csv.setQuote('"');
        csv.setFile(TestHelper.loadFileFromResources("correct-products.csv"));

        given()
            .body(csv).
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
    public void validation_problems_with_importing_csv() {
        CSVUploadDTO csv = new CSVUploadDTO();
        csv.setSeparator(';');
        csv.setQuote('"');
        csv.setFile(TestHelper.loadFileFromResources("incorrect-products.csv"));

        given()
            .body(csv).
        when()
            .post(Consts.Paths.Product.UPLOAD_CSV).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("result", equalTo("Failed to upload CSV file correctly, please see details."));
    }

}
