package io.inprice.scrapper.api.rest;

import com.google.gson.internal.LinkedTreeMap;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Global;
import io.inprice.scrapper.api.helpers.TestHelper;
import io.restassured.response.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

public class ProductCSVImportTest {

    private static final String PATH = Consts.Paths.Product.IMPORT_CSV;
    private static final String ROOT = "csv";
    private static final String TYPE = "CSV file";
    
    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true, true);
    }

    @Test
    public void everything_should_be_ok() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/correct-products.csv")).
        when()
            .post(PATH).
        then()
            .body("result", equalTo(TYPE + " has been successfully uploaded.")).assertThat()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("totalCount", equalTo(2)).assertThat()
            .body("insertCount", equalTo(2)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problemCount", equalTo(0)).assertThat();
    }

    @Test
    public void everything_should_be_ok_with_duplicate_codes() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/duplicate-products.csv")).
        when()
            .post(PATH).
        then()
            .body("result", equalTo(TYPE + " has been successfully uploaded.")).assertThat()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("totalCount", equalTo(3)).assertThat()
            .body("insertCount", equalTo(2)).assertThat()
            .body("duplicateCount", equalTo(1)).assertThat()
            .body("problemCount", equalTo(0)).assertThat();
    }

    @Test
    public void validation_problems() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/incorrect-products.csv")).
        when()
            .post(PATH).
        then()
            .body("result", equalTo("Failed to import " + TYPE + ", please see details!")).assertThat()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat();
    }

    @Test
    public void format_error() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/format-error-products.csv")).
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
            .body("result", equalTo(TYPE + " is empty!"))
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat();
    }

    @Test
    public void field_validation_problems() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/half-correct-products.csv")).
        when()
            .post(PATH).
        then()
            .body("result", equalTo(TYPE + " has been uploaded. However, some problems occurred. Please see details.")).assertThat()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("totalCount", equalTo(3)).assertThat()
            .body("insertCount", equalTo(1)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problemCount", equalTo(2)).assertThat()
            .body("problemList[0]", equalTo("002: Price must be greater than zero!")).assertThat()
            .body("problemList[1]", equalTo("003: Product name must be between 3 and 500 chars!")).assertThat();
    }

    @Test
    public void missing_column_problems() {
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/missing-column-products.csv")).
        when()
            .post(PATH).
        then()
            .body("result", equalTo("Failed to import " + TYPE + ", please see details!")).assertThat()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("totalCount", equalTo(2)).assertThat()
            .body("insertCount", equalTo(0)).assertThat()
            .body("duplicateCount", equalTo(0)).assertThat()
            .body("problemCount", equalTo(2)).assertThat()
            .body("problemList[0]", equalTo("001: There must be 5 columns in each row!. Column separator is comma ,")).assertThat()
            .body("problemList[1]", equalTo("002: There must be 5 columns in each row!. Column separator is comma ,")).assertThat();
    }

    @Test
    public void limit_problem_for_two_cases() {
        //case 1
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/too-many-products.csv")).
        when()
            .post(PATH).
        then()
            .body("result", equalTo(TYPE + " has been uploaded. However, some problems occurred. Please see details.")).assertThat()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("totalCount", equalTo(31)).assertThat()
            .body("problemCount", equalTo(1)).assertThat()
            .body("problemList[0]", equalTo("031: You have reached your plan's maximum product limit.")).assertThat();

        //case 2
        given()
            .body(TestHelper.loadFileFromResources(ROOT + "/correct-products.csv")).
        when()
            .post(PATH).
        then()
            .body("result", equalTo("You have already reached your plan's maximum product limit.")).assertThat()
            .statusCode(HttpStatus.TOO_MANY_REQUESTS_429).assertThat();
    }


    @After
    public void clearAllProducts() {
        Response res =
            when()
                .get(Consts.Paths.Product.BASE + "s").
            then()
                .extract().
            response();

        Map map = Global.gson.fromJson(res.asString(), Map.class);
        List<Long> idList = new ArrayList<>();
        if (map.containsKey("models")) {
            List<LinkedTreeMap<String, Object>> valueList = (List) map.get("models");
            if (valueList.size() > 0) {
                for (LinkedTreeMap<String, Object> ltm: valueList) {
                    double dbl = (double) ltm.get("id");
                    idList.add(Double.valueOf(dbl).longValue());
                }
            }
        }

        if (idList.size() > 0) {
            for (long id: idList) {
                delete(Consts.Paths.Product.BASE + "/" + id);
            }
        }
    }

}
