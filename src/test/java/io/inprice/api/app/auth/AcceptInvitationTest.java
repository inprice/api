package io.inprice.api.app.auth;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.app.utils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * TODO: new tests for successful and failed cases of accepting new invitation must be added after MemberService tests implemented!
 * 
 * No need to check passwords again here since it is already done in Login and ForgotPassword test classes
 *  
 * @author mdpinar
 * @since 2021-07-01
 */
@RunWith(JUnit4.class)
public class AcceptInvitationTest {

	private static final String SERVICE_ENDPOINT = "/accept-invitation";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Request_body_is_invalid_WITH_no_body() {
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
	public void Invalid_token_WITH_wrong_token() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("XYZ-123", "1234", "1234"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(130, json.getInt("status"));
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

	private JSONObject createBody(String token, String password, String repeatPassword) {
		JSONObject body = new JSONObject();
		if (token != null) body.put("token", token);
		if (password != null) body.put("password", password);
		if (repeatPassword != null) body.put("repeatPassword", repeatPassword);
		return body;
	}

}
