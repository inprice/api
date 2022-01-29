package io.inprice.api.app.user;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of UserController.updateInfo(UserDTO) 
 * 
 * @author mdpinar
 * @since 2021-07-10
 */
@RunWith(JUnit4.class)
public class UpdateInfoTest {

	private static final String SERVICE_ENDPOINT = "/user/update-info";
	private static final String TIMEZONE = "Europe/Dublin";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(createBody("New Name", TIMEZONE))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Professional_plan_and_one_extra_user.EDITOR());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Full_Name_cannot_be_empty() {
		JSONObject json = callTheServiceWith(null, TIMEZONE);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Full Name cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Full_Name_must_be_between_3_and_70_chars_WITH_shorter_name() {
		JSONObject json = callTheServiceWith("XY", TIMEZONE);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Full Name must be between 3 - 70 chars!", json.getString("reason"));
	}

	@Test
	public void Full_Name_must_be_between_3_and_70_chars_WITH_longer_name() {
		JSONObject json = callTheServiceWith(RandomStringUtils.randomAlphabetic(71), TIMEZONE);

		assertEquals(400, json.getInt("status"));
		assertEquals("Full Name must be between 3 - 70 chars!", json.getString("reason"));
	}

	@Test
	public void Timezone_cannot_be_empty() {
		JSONObject json = callTheServiceWith("New name", null);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Timezone cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Unknown_time_zone() {
		JSONObject json = callTheServiceWith("New name", "Unknown");
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Unknown timezone!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody("New name", TIMEZONE))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.get("reason"));
	}

	@Test
	public void Everything_must_be_ok() {
		JSONObject json = callTheServiceWith("Editor's new name", TIMEZONE);
		
		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.get("reason"));
	}

	private JSONObject callTheServiceWith(String name, String timezone) {
		//creating the body
		JSONObject body = createBody(name, timezone);

		//login with an admin
		Cookies cookies = TestUtils.login(TestWorkspaces.Professional_plan_and_one_extra_user.EDITOR());

		//making service call
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		
		//logout
		TestUtils.logout(cookies);

		//returning the result to be tested
		return res.getBody().getObject();
	}

	private JSONObject createBody(String name, String timezone) {
		JSONObject body = new JSONObject();
		if (name != null) body.put("fullName", name);
		if (timezone != null) body.put("timezone", timezone);
		return body;
	}

}
