package io.inprice.scrapper.api.rest;

import io.inprice.scrapper.api.dto.ProductDTO;
import io.inprice.scrapper.api.helpers.Consts;
import io.inprice.scrapper.api.helpers.TestHelper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

//todo: workspace id in headers must be tested
//todo: duplicate code in insert and update must be tested
public class ProductTest {

    @BeforeClass
    public static void setup() {
        TestHelper.setup(true, true, true);

        //adding a product to crud on it
        given()
            .body(TestHelper.getProductDTO()).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_inserting() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setCode(product.getCode()+"P1");

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_finding() {
        final int id = 1;

        when()
            .get(Consts.Paths.Product.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("model.id", equalTo(id));
    }

    @Test
    public void everything_should_be_ok_with_toggle_status() {
        final int id = 1;

        when()
            .put(Consts.Paths.Product.TOGGLE_STATUS + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_deleting() {
        final long id = 2;

        final ProductDTO product = TestHelper.getProductDTO();
        product.setCode(product.getCode()+"P1");
        product.setName("A NEW PRODUCT");

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));

        when()
            .delete(Consts.Paths.Product.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void everything_should_be_ok_with_listing() {
        when()
            .get(Consts.Paths.Product.BASE + "s").
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("models.size", greaterThan(0)); //since we have a default product inserted at the beginning
    }

    @Test
    public void everything_should_be_ok_with_updating() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setId(1L);
        product.setName("New name should be updated!");

        given()
            .body(product).
        when()
            .put(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.OK_200).assertThat()
            .body("result", equalTo("OK"));
    }

    @Test
    public void missing_workspace_with_inserting() {
        TestHelper.loginAsUser();

        final ProductDTO product = TestHelper.getProductDTO();

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body(equalTo("Workspace is missing!"));

        TestHelper.loginAsAdmin(true);
    }

    @Test
    public void missing_workspace_with_finding() {
        TestHelper.loginAsUser();

        final int id = 1;

        when()
            .get(Consts.Paths.Product.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body(equalTo("Workspace is missing!"));

        TestHelper.loginAsAdmin(true);
    }

    @Test
    public void missing_workspace_with_toggle_status() {
        TestHelper.loginAsUser();

        final int id = 1;

        when()
            .put(Consts.Paths.Product.TOGGLE_STATUS + "/" + id).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body(equalTo("Workspace is missing!"));

        TestHelper.loginAsAdmin(true);
    }

    @Test
    public void missing_workspace_with_deleting() {
        TestHelper.loginAsUser();

        final long id = 2;

        when()
            .delete(Consts.Paths.Product.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body(equalTo("Workspace is missing!"));

        TestHelper.loginAsAdmin(true);
    }

    @Test
    public void missing_workspace_ok_with_listing() {
        TestHelper.loginAsUser();

        when()
            .get(Consts.Paths.Product.BASE + "s").
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body(equalTo("Workspace is missing!"));

        TestHelper.loginAsAdmin(true);
    }

    @Test
    public void missing_workspace_ok_with_updating() {
        TestHelper.loginAsUser();

        final ProductDTO product = TestHelper.getProductDTO();
        product.setId(1L);
        product.setName("New name shouldn't be updated!");

        given()
            .body(product).
        when()
            .put(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body(equalTo("Workspace is missing!"));

        TestHelper.loginAsAdmin(true);
    }

    @Test
    public void product_not_found_when_get_with_a_wrong_id() {
        when()
            .get(Consts.Paths.Product.BASE + "/0").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Product not found!"));
    }

    @Test
    public void product_not_found_when_delete_with_a_wrong_id() {
        when()
            .delete(Consts.Paths.Product.BASE + "/0").
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Product not found!"));
    }

    @Test
    public void invalid_data_for_product() {
        given()
            .body("wrong body!").
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.NOT_ACCEPTABLE_406).assertThat()
            .body("result", equalTo("Invalid data for product!"));
    }

    @Test
    public void product_code_length_is_out_of_range_if_greater_than_120() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setCode(StringUtils.repeat('A', 121));

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Product code can be up to 120 chars!"));
    }

    @Test
    public void brand_length_is_out_of_range_if_greater_than_100() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setBrand(StringUtils.repeat('A', 101));

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Brand can be up to 100 chars!"));
    }

    @Test
    public void category_length_is_out_of_range_if_greater_than_100() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setCategory(StringUtils.repeat('A', 101));

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Category can be up to 100 chars!"));
    }

    @Test
    public void product_code_cannot_be_null() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setCode(null);

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Product code cannot be null!"));
    }

    @Test
    public void product_code_length_is_out_of_range_if_less_than_2() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setName("A");

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Product code must be between 2 and 500 chars!"));
    }

    @Test
    public void product_code_length_is_out_of_range_if_greater_than_500() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setCode(StringUtils.repeat('A', 121));

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Product code must be between 2 and 500 chars!"));
    }

    @Test
    public void product_name_cannot_be_null() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setName(null);

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Product name cannot be null!"));
    }

    @Test
    public void product_name_length_is_out_of_range_if_less_than_3() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setName("AA");

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Product name must be between 3 and 500 chars!"));
    }

    @Test
    public void product_name_length_is_out_of_range_if_greater_than_500() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setName(StringUtils.repeat('A', 501));

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Product name must be between 3 and 500 chars!"));
    }

    @Test
    public void product_not_found_when_update_with_a_wrong_id() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setId(0L);

        given()
            .body(product).
        when()
            .put(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.NOT_FOUND_404).assertThat()
            .body("result", equalTo("Product not found!"));
    }

    @Test
    public void price_must_be_greater_than_zero_for_null_value() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setPrice(null);

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Price must be greater than zero!"));
    }

    @Test
    public void price_must_be_greater_than_zero_for_lesser_than_one() {
        final ProductDTO product = TestHelper.getProductDTO();
        product.setPrice(BigDecimal.ZERO);

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.BAD_REQUEST_400).assertThat()
            .body("problems.reason[0]", equalTo("Price must be greater than zero!"));
    }

    @Test
    public void permission_problem_for_a_user_with_inserting() {
        TestHelper.loginAsUser(true);

        final ProductDTO product = TestHelper.getProductDTO();

        given()
            .body(product).
        when()
            .post(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body("result", equalTo("User has no permission to save a product!"));

        TestHelper.loginAsAdmin(true);
    }

    @Test
    public void permission_problem_for_a_user_with_updating() {
        TestHelper.loginAsUser(true);

        final ProductDTO product = TestHelper.getProductDTO();

        given()
            .body(product).
        when()
            .put(Consts.Paths.Product.BASE).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body("result", equalTo("User has no permission to save a product!"));

        TestHelper.loginAsAdmin(true);
    }

    @Test
    public void permission_problem_for_a_user_with_deleting() {
        TestHelper.loginAsUser(true);

        final int id = 1;

        when()
            .delete(Consts.Paths.Product.BASE + "/" + id).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body("result", equalTo("User has no permission to delete a product!"));

        TestHelper.loginAsAdmin(true);
    }

    @Test
    public void permission_problem_for_a_user_with_toggle_status() {
        TestHelper.loginAsUser(true);

        final int id = 1;

        when()
            .put(Consts.Paths.Product.TOGGLE_STATUS + "/" + id).
        then()
            .statusCode(HttpStatus.FORBIDDEN_403).assertThat()
            .body("result", equalTo("User has no permission to toggle a product's status!"));

        TestHelper.loginAsAdmin(true);
    }

}
