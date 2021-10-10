package io.inprice.api.app.definitions.category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 * Tests the functionality of CategoryController.update(IdTextDTO)
 * 
 * @author mdpinar
 * @since 2021-09-19
 */
@RunWith(JUnit4.class)
public class UpdateTest {

	private static final String SERVICE_ENDPOINT = "/def/category";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
				.put("id", "1")
  			.put("text", "GROCERY");

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
	public void Category_not_found_WITH_null_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("id");

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Category not found!", json.getString("reason"));
	}

	@Test
	public void Category_not_found_WHEN_trying_to_update_someone_elses_category() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", 3); //here is important!!!

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Category not found!", json.getString("reason"));
	}

	@Test
	public void Name_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("text");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Name cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Name_must_be_between_2_and_50_chars_WITH_shorter_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("text", "A");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Name must be between 2 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Name_must_be_between_2_and_50_chars_WITH_longer_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("text", RandomStringUtils.randomAlphabetic(51));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Name must be between 2 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, SAMPLE_BODY);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Pro_plan_with_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
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
	public void This_category_has_already_been_added() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("text", "TOOLS");

		JSONObject json = callTheService(body);

		assertEquals(877, json.getInt("status"));
    assertEquals("This category has already been added!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		JSONObject json = callTheService(body);

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
