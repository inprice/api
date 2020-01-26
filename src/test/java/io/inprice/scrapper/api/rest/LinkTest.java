package io.inprice.scrapper.api.rest;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import java.util.List;
import java.util.Map;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import io.inprice.scrapper.api.dto.LinkDTO;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.Responses;
import io.inprice.scrapper.api.helpers.TestHelper;
import io.restassured.response.Response;

public class LinkTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true);

        //adding a product to crud on it
        given()
            .body(TestHelper.getProductDTO()).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void everything_should_be_ok_with_inserting() {
        final LinkDTO link = TestHelper.getLinkDTO(1L);
        link.setUrl(link.getUrl()+"L0");

        given()
            .body(link).
        when()
            .post(Consts.Paths.Link.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void everything_should_be_ok_with_finding() {
        final int id = 1;

        when()
            .get(Consts.Paths.Link.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()))
            .body("model.id", equalTo(id));
    }

    @Test
    public void everything_should_be_ok_with_deleting() {
        TestHelper.loginAsAdmin();

        final long id = 2;

        LinkDTO l1 = TestHelper.getLinkDTO(1L);
        l1.setUrl(l1.getUrl()+"L1");

        LinkDTO l2 = TestHelper.getLinkDTO(1L);
        l2.setUrl(l2.getUrl()+"L2");

        given()
            .body(l1).
        when()
            .post(Consts.Paths.Link.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));

        given()
            .body(l2).
        when()
            .post(Consts.Paths.Link.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));

        when()
            .delete(Consts.Paths.Link.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("status", equalTo(Responses.OK.getStatus()));
    }

    @Test
    public void everything_should_be_ok_with_listing() {
        final long id = 1;

        when()
            .get(Consts.Paths.Link.BASE + "s/" + id).
        then()
            .body("status", equalTo(Responses.OK.getStatus()))
            .body("models.size", greaterThan(0)) //since we have a default link inserted at the beginning
            .statusCode(HttpStatus.OK_200).assertThat();
    }

    @Test
    public void link_not_found_when_get_with_a_wrong_id() {
        when()
            .get(Consts.Paths.Link.BASE + "/0").
        then()
            .body("status", equalTo(Responses.NotFound.LINK.getStatus()));
    }

    @Test
    public void link_not_found_when_delete_with_a_wrong_id() {
        when()
            .delete(Consts.Paths.Link.BASE + "/0").
        then()
            .body("status", equalTo(Responses.Invalid.LINK.getStatus()));
    }

    @Test
    public void product_not_found_for_a_wrong_product_id_with_listing() {
        final long id = 9;

        when()
            .get(Consts.Paths.Link.BASE + "s/" + id).
        then()
            .body("status", equalTo(Responses.NotFound.PRODUCT.getStatus()));
    }

    @Test
    public void invalid_data_for_link() {
        given()
            .body("wrong body!").
        when()
            .post(Consts.Paths.Link.BASE).
        then()
            .body("status", equalTo(Responses.Invalid.LINK.getStatus()));
    }

    @Test
    public void invalid_url_for_null_value() {
        final LinkDTO link = TestHelper.getLinkDTO(1L);
        link.setUrl(null);

        given()
            .body(link).
        when()
            .post(Consts.Paths.Link.BASE).
        then()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Invalid URL!"));
    }

    @Test
    public void invalid_url_for_wrong_value() {
        final LinkDTO link = TestHelper.getLinkDTO(1L);
        link.setUrl("wrong url!");

        given()
            .body(link).
        when()
            .post(Consts.Paths.Link.BASE).
        then()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Invalid URL!"));
    }

    @Test
    public void invalid_url_for_invalid_format() {
        final LinkDTO link = TestHelper.getLinkDTO(1L);
        link.setUrl("http://www.amazon");

        given()
            .body(link).
        when()
            .post(Consts.Paths.Link.BASE).
        then()
            .body("status", equalTo(Responses.DataProblem.FORM_VALIDATION.getStatus()))
            .body("problems.reason[0]", equalTo("Invalid URL!"));
    }

    @Test
    public void permission_problem_for_a_user_with_inserting() {
        TestHelper.loginAsUser();

        final LinkDTO link = TestHelper.getLinkDTO(1L);

        given()
            .body(link).
        when()
            .post(Consts.Paths.Link.BASE).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body(equalTo("Unauthorized user!"));

        TestHelper.loginAsAdmin();
    }

    @Test
    public void permission_problem_for_a_user_with_deleting() {
        TestHelper.loginAsUser();

        final int id = 1;

        when()
            .delete(Consts.Paths.Link.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body(equalTo("Unauthorized user!"));

        TestHelper.loginAsAdmin();
    }

    @Test
    public void permission_problem_for_a_user_with_changing_status_to_PAUSED() {
        TestHelper.loginAsUser();

        final int id = 1;
        final int productId = 1;

        when()
            .put(Consts.Paths.Link.PAUSE + "/" + id + "?product_id=" + productId).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body(equalTo("Unauthorized user!"));

        TestHelper.loginAsAdmin();
    }

    @Test
    public void permission_problem_for_a_user_with_changing_status_to_RENEWED() {
        TestHelper.loginAsUser();

        final int id = 1;
        final int productId = 1;

        when()
            .put(Consts.Paths.Link.RENEW + "/" + id + "?product_id=" + productId).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body(equalTo("Unauthorized user!"));

        TestHelper.loginAsAdmin();
    }

    @Test
    public void permission_problem_for_a_user_with_changing_status_to_RESUMED() {
        TestHelper.loginAsUser();

        final int id = 1;
        final int productId = 1;

        when()
            .put(Consts.Paths.Link.RESUME + "/" + id + "?product_id=" + productId).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body(equalTo("Unauthorized user!"));

        TestHelper.loginAsAdmin();
    }

    @Test
    public void failed_to_change_status_for_a_wrong_product_id_with_changing_status_to_PAUSED() {
        final Integer linkId = insertAndGetId();
        final int productId = 9;

        when()
            .put(Consts.Paths.Link.PAUSE + "/" + linkId + "?product_id=" + productId).
        then()
            .body("status", equalTo(Responses.Invalid.PRODUCT.getStatus()));

        TestHelper.loginAsAdmin();
    }

    @Test
    public void failed_to_change_status_for_a_wrong_product_id_with_changing_status_to_RENEWED() {
        final int id = 1;
        final int productId = 9;

        when()
            .put(Consts.Paths.Link.RENEW + "/" + id + "?product_id=" + productId).
        then()
            .body("status", equalTo(Responses.Invalid.PRODUCT.getStatus()));

        TestHelper.loginAsAdmin();
    }

    @Test
    public void failed_to_change_status_for_a_wrong_product_id_with_changing_status_to_RESUMED() {
        final int id = 1;
        final int productId = 9;

        when()
            .put(Consts.Paths.Link.RESUME + "/" + id + "?product_id=" + productId).
        then()
            .body("status", equalTo(Responses.Invalid.PRODUCT.getStatus()));
    }

    private Integer insertAndGetId() {
        final long productId = 1L;

        final LinkDTO link = TestHelper.getLinkDTO(productId);
        link.setUrl(link.getUrl()+"LM");

        given()
            .body(link).
        when()
            .post(Consts.Paths.Link.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat();

        Response res =
            when()
                .get(Consts.Paths.Link.BASE + "s/" + productId).
            then()
                .body("status", equalTo(Responses.OK.getStatus())).
            extract().
        response();

        List<Map<String, Object>> models = res.body().jsonPath().getList("models");

        if (models != null && models.size() > 0) {
            return (int) models.get(models.size() - 1).get("id");
        }

        return null;
    }

}
