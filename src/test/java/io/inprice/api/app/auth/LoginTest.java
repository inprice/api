package io.inprice.api.app.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of AuthController.login(LoginDTO)
 * 
 * @author mdpinar
 * @since 2021-06-28
 */
@RunWith(JUnit4.class)
public class LoginTest {

	private static final String SERVICE_ENDPOINT = "/login";

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
	public void Email_address_cannot_be_empty_WITH_empty_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody(null, "1234-AB"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Password_cannot_be_empty_WITH_empty_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("admin@inprice.io", null))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Password cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Invalid_email_address_WITH_wrong_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("admininprice.io", "1234-AB"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Invalid email address!", json.getString("reason"));
	}

	@Test
	public void Email_address_must_be_between_8_and_128_chars_WITH_shorter_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("a@xy.io", "1234-AB"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address must be between 8 - 128 chars!", json.getString("reason"));
	}

	@Test
	public void Email_address_must_be_between_8_and_128_chars_WITH_longer_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody(RandomStringUtils.randomAlphabetic(118)+"@inprice.io", "1234-AB"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address must be between 8 - 128 chars!", json.getString("reason"));
	}

	@Test
	public void Password_length_must_be_between_6_and_16_chars_WITH_shorter_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("admin@inprice.io", "1235A"))
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
    assertEquals("Password length must be between 6 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Password_length_must_be_between_6_and_16_chars_WITH_longer_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("admin@inprice.io", RandomStringUtils.randomAlphabetic(17)))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Password length must be between 6 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Invalid_email_or_password_WITH_wrong_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("admin@inprice.io", "1234-AC"))
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(113, json.getInt("status"));
    assertEquals("Invalid email or password!", json.getString("reason"));
	}

	@Test
	public void Banned_user() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.BANNED_USER)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Banned user!", json.getString("reason"));
	}

	@Test
	public void Your_account_has_been_locked_for_5_minutes_WITH_wrong_credentials_FOR_three_attempts() {
		JSONObject user = new JSONObject();
		user.put("email", TestWorkspaces.Cancelled_Professional_plan.EDITOR().get("email"));
		user.put("password", "WRONG-PASS");

		for (int i = 0; i < 4; i++) {
			HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
				.body(user)
				.asJson();
			TestUtils.logout(res.getCookies());

			JSONObject json = res.getBody().getObject();
			
			if (i < 3) {
				assertEquals(113, json.getInt("status"));
		    assertEquals("Invalid email or password!", json.getString("reason"));
			} else {
				assertEquals(400, json.getInt("status"));
		    assertTrue(json.getString("reason").startsWith("Your account has been locked for 5 minutes"));
			}
		}
	}

	@Test
	public void Everything_must_be_OK_WITH_correct_credentials() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(TestWorkspaces.Standard_plan_and_no_extra_user.ADMIN())
			.asJson();
		TestUtils.logout(res.getCookies());

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
    assertEquals(1, data.getJSONArray("sessions").length());
	}

	@Test
	public void Everything_must_be_OK_and_must_have_multiple_sessions_WITH_correct_credentials() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.USER_HAVING_THREE_MEMBERSHIPS)
			.asJson();
		TestUtils.logout(res.getCookies());

		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONObject("data"));
		
		JSONObject data = json.getJSONObject("data");
    assertEquals(2, data.getJSONArray("sessions").length());
	}

	@Test
	public void Everything_must_be_OK_WITH_correct_credentials_OF_superuser() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.SUPER_USER)
			.asJson();
		TestUtils.logout(res.getCookies());

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
    assertEquals(1, data.getJSONArray("sessions").length());
	}

	private JSONObject createBody(String email, String password) {
		JSONObject body = new JSONObject();
		if (email != null) body.put("email", email);
		if (password != null) body.put("password", password);
		return body;
	}

}
