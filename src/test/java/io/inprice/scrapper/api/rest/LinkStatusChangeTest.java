package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.helpers.TestHelper;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;

public class LinkStatusChangeTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true, true);
        TestHelper.runScript("fixtures/links_in_various_statuses.sql");
    }

    @Test
    public void ok_with_ALL_SUITABLE_STATUSES_tobe_PAUSED() {
        for (int i = 1; i < 12; i++) {
            //System.out.println(i);
            when()
                .put(Consts.Paths.Link.PAUSE + "/" + i + "?product_id=1").
            then()
                .statusCode(HttpStatus.OK_200).assertThat()
                .body("status", equalTo(Responses.OK.getStatus()));
        }
    }


    @Test
    public void problem_with_ALL_UNSUITABLE_STATUSES_tobe_PAUSED() {
        for (int i = 12; i < 19; i++) {
            //System.out.println(i);
            when()
                .put(Consts.Paths.Link.PAUSE + "/" + i + "?product_id=1").
            then()
                .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
                .body("status", equalTo(Responses.DataProblem.NOT_SUITABLE.getStatus()));
        }
    }

    @Test
    public void ok_with_PAUSED_to_RESUMED() {
        when()
            .put(Consts.Paths.Link.RESUME + "/" + 19 + "?product_id=1").
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }


    @Test
    public void problem_with_ALL_UNSUITABLE_STATUSES_tobe_RESUMED() {
        for (int i = 20; i < 36; i++) {
            //System.out.println(i);
            when()
                .put(Consts.Paths.Link.RESUME + "/" + i + "?product_id=1").
            then()
                .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
                .body("status", equalTo(Responses.DataProblem.NOT_SUITABLE.getStatus()));
        }
    }

    @Test
    public void ok_with_AVAILABLE_to_RENEWED() {
        when()
            .put(Consts.Paths.Link.RENEW + "/" + 37 + "?product_id=1").
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }


    @Test
    public void problem_with_ALL_UNSUITABLE_STATUSES_tobe_RENEWED() {
        for (int i = 38; i < 54; i++) {
            //System.out.println(i);
            when()
                .put(Consts.Paths.Link.RENEW + "/" + i + "?product_id=1").
            then()
                .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
                .body("status", equalTo(Responses.DataProblem.NOT_SUITABLE.getStatus()));
        }
    }

}
