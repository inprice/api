package io.inprice.api.app.product;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestUtils;
import io.inprice.api.utils.TestWorkspaces;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of ProductController.insert(ProductDTO)
 * 
 * @author mdpinar
 * @since 2021-07-20
 */
@RunWith(JUnit4.class)
public class InsertTest {

	private static final String SERVICE_ENDPOINT = "/product";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
	    	.put("sku", "A123")
	    	.put("name", "NEW PRODUCT")
				.put("price", 5);

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(SAMPLE_BODY)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Sku_must_be_between_3_50_chars_WITH_shorter_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("sku", "AB");

		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
    assertEquals("Sku must be between 3 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Sku_must_be_between_3_50_chars_WITH_longer_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("sku", RandomStringUtils.randomAlphabetic(51));

		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
    assertEquals("Sku must be between 3 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Name_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("name");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Name cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Name_must_be_between_3_and_250_chars_WITH_shorter_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("name", "AB");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Name must be between 3 - 250 chars!", json.getString("reason"));
	}

	@Test
	public void Name_must_be_between_3_and_250_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("name", RandomStringUtils.randomAlphabetic(251));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Name must be between 3 - 250 chars!", json.getString("reason"));
	}

	@Test
	public void Price_must_be_greater_than_zero_FOR_a_value_of_less_than_zero() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("price", -5.0);
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Price must be greater than zero!", json.getString("reason"));
	}

	@Test
	public void Price_is_out_of_reasonable_range_FOR_a_value_of_10m() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("price", 100_000_000);
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Price is out of reasonable range!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, SAMPLE_BODY, 0);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER(), SAMPLE_BODY, 0);

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_already_have_a_product_having_the_same_sku_or_name_FOR_sku() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("sku", "F-1");

		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(), body, 0);

		assertEquals(875, json.getInt("status"));
		assertEquals("You already have a product having the same sku!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_duplicate_name_WITH_editor() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("sku", "T-1");
		body.put("name", "Product K of Workspace-F");

		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(), body, 0);
		assertEquals(200, json.getInt("status"));
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("sku", "Z-1");
		body.put("name", "Editor is trying to define a new product!");

		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(), body, 0);
		assertEquals(200, json.getInt("status"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("name", "Admin is trying to define a new product!");

		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.ADMIN(), body, 0);
		assertEquals(200, json.getInt("status"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestWorkspaces.Pro_plan_with_no_user.ADMIN(), body, 0);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //attention pls!
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
