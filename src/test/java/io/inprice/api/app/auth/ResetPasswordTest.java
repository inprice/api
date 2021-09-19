package io.inprice.api.app.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of AuthController.resetPassword(PasswordDTO)
 * 
 * @author mdpinar
 * @since 2021-07-01
 */
@RunWith(JUnit4.class)
public class ResetPasswordTest {

	private static final String SERVICE_ENDPOINT = "/reset-password";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Invalid_token_WITH_empty_token() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody(null, "1234", "1234"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Invalid token!", json.getString("reason"));
	}

	@Test
	public void Password_cannot_be_empty_WITH_empty_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("XYZ-123", null, "1234"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Password cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Password_length_must_be_between_4_and_16_chars_WITH_shorter_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("XYZ-123", "123", "123"))
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
    assertEquals("Password length must be between 4 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Password_length_must_be_between_4_and_16_chars_WITH_longer_password() {
		final String password = RandomStringUtils.randomAlphabetic(17);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("XYZ-123", password, password))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Password length must be between 4 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Passwords_are_mismatch_WITH_different_passwords() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("XYZ-123", "1234", "1235"))
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
    assertEquals("Passwords are mismatch!", json.getString("reason"));
	}

	@Test
	public void Your_password_is_already_reset_WITH_multiple_reset_calls() {
		//first we need to call forgot password to get a token which is needed below!
		HttpResponse<JsonNode> res = Unirest.post("/forgot-password")
			.body(TestWorkspaces.Pro_plan_with_two_extra_users.VIEWER())
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONObject("data"));

		JSONObject data = json.getJSONObject("data");

		String token = data.getString("token");
		assertNotNull(token);
		
		res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody(token, TestWorkspaces.Pro_plan_with_two_extra_users.VIEWER()))
			.asJson();

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));

		res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody(token, TestWorkspaces.Pro_plan_with_two_extra_users.VIEWER()))
			.asJson();

		json = res.getBody().getObject();

		assertEquals(815, json.getInt("status"));
		assertEquals("Your password is already reset!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_OK_WITH_correct_credentials() {
		//first we need to call forgot password to get a token which is needed below!
		HttpResponse<JsonNode> res = Unirest.post("/forgot-password")
			.body(TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR())
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONObject("data"));

		JSONObject data = json.getJSONObject("data");

		String token = data.getString("token");
		assertNotNull(token);
		
		res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody(token, TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR()))
			.asJson();
		TestUtils.logout(res.getCookies());

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
	}

	private JSONObject createBody(String token, JSONObject user) {
		return createBody(token, user.getString("password"), user.getString("password"));
	}

	private JSONObject createBody(String token, String password, String repeatPassword) {
		JSONObject body = new JSONObject();
		if (token != null) body.put("token", token);
		if (password != null) body.put("password", password);
		if (repeatPassword != null) body.put("repeatPassword", repeatPassword);
		return body;
	}

}
