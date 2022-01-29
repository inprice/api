package io.inprice.api.app.smartprice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
 * Tests the functionality of SmartPriceController.update(SmartPriceDTO)
 * 
 * @author mdpinar
 * @since 2021-10-16
 */
@RunWith(JUnit4.class)
public class UpdateTest {

	private static final String SERVICE_ENDPOINT = "/smart-price";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
				.put("id", "1")
				.put("name", "New formula")
				.put("formula", "min((p*1.10)+0.75,a)")
				.put("lowerLimitFormula", "(i-(i*10/100))")
				.put("upperLimitFormula", "a+1.50")
				;

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
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
	public void Smart_price_not_found_WITH_null_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("id");

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Smart price not found!", json.getString("reason"));
	}

	@Test
	public void Smart_price_not_found_WHEN_trying_to_update_someone_elses_brand() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", 3); //important!!!
		body.put("name", "Updated name");

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Smart price not found!", json.getString("reason"));
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
	public void Name_can_be_up_to_70_chars_WITH_longer_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("name", RandomStringUtils.randomAlphabetic(71));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Name can be up to 70 chars!", json.getString("reason"));
	}

	@Test
	public void Formula_cannot_be_empty_WITH_empty_formula() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("formula", "");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Formula cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Formula_cannot_be_empty_WITHOUT_formula() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("formula");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Formula cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Formula_can_be_up_to_255_chars_WITH_longer_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("formula", RandomStringUtils.randomAlphabetic(256));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Formula can be up to 255 chars!", json.getString("reason"));
	}

	@Test
	public void Division_by_zero_error_FOR_formula() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("formula", "i/0");
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertTrue(json.getString("reason").contains("Division by zero"));
	}

	@Test
	public void Paranthesis_error_FOR_lower_limit_formula() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("lowerLimitFormula", "(i+10");
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertTrue(json.getString("reason").contains("parentheses"));
	}

	@Test
	public void Lower_Limit_Formula_can_be_up_to_255_chars_WITH_longer_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("lowerLimitFormula", RandomStringUtils.randomAlphabetic(256));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Lower Limit Formula can be up to 255 chars!", json.getString("reason"));
	}

	@Test
	public void Upper_Limit_Formula_can_be_up_to_255_chars_WITH_longer_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("upperLimitFormula", RandomStringUtils.randomAlphabetic(256));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Upper Limit Formula can be up to 255 chars!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, SAMPLE_BODY);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(SAMPLE_BODY)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void This_formula_has_already_been_added() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("name", "Other Formula");

		JSONObject json = callTheService(body);

		assertEquals(881, json.getInt("status"));
    assertEquals("This formula has already been added!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", 4L);
		body.put("formula", "a/2+i/2+1.12"); //this must be cause re-calculated the product's suggested price

		JSONObject json = callTheService(TestWorkspaces.Premium_plan_with_no_user.ADMIN(), body);

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN(), body);
	}

	private JSONObject callTheService(JSONObject user, JSONObject body) {
		return callTheService(user, body, 0);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
