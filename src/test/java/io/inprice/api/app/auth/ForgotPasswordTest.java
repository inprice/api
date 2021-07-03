package io.inprice.api.app.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.app.utils.Fixtures;
import io.inprice.api.app.utils.TestRole;
import io.inprice.api.app.utils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * 
 * @author mdpinar
 * @since 2021-07-01
 */
@RunWith(JUnit4.class)
public class ForgotPasswordTest {

	private static final String SERVICE_ENDPOINT = "/forgot-password";

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
	public void Email_address_cannot_be_empty_WITH_empty_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody(null))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Invalid_email_address_WITH_wrong_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("admininprice.io"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Invalid email address!", json.getString("reason"));
	}

	@Test
	public void You_will_be_receiving_an_email_after_verification_of_your_email_WITH_nonexistent_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("someone@inprice.io"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("You will be receiving an email after verification of your email.", json.getString("reason"));
	}

	@Test
	public void Email_address_must_be_between_8_and_128_chars_WITH_shorter_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody("a@xy.io"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address must be between 8 and 128 chars!", json.getString("reason"));
	}

	@Test
	public void Email_address_must_be_between_8_and_128_chars_WITH_longer_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createBody(RandomStringUtils.randomAlphabetic(118)+"@inprice.io"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address must be between 8 and 128 chars!", json.getString("reason"));
	}

	@Test
	public void Banned_user_FOR_banned_user() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.BANNED_USER)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Banned user!", json.getString("reason"));
	}

	@Test
	public void User_is_not_suitable_for_this_operation_FOR_super_user() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.SUPER_USER)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(912, json.getInt("status"));
		assertEquals("User is not suitable for this operation!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_OK_WITH_correct_credentials() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.NORMAL_USER(TestRole.ADMIN))
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONObject("data"));
	}

	@Test
	public void This_email_is_already_requested_please_wait_some_time_to_try_again_WITH_correct_credentials() {
		//first request
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.NORMAL_USER(TestRole.VIEWER))
			.asJson();
		
		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		
		//second request
		res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.NORMAL_USER(TestRole.VIEWER))
			.asJson();
			
		json = res.getBody().getObject();
		
		assertEquals(814, json.getInt("status"));
		assertEquals("This email is already requested, please wait some time to try again!", json.getString("reason"));
	}
	
	private JSONObject createBody(String email) {
		JSONObject body = new JSONObject();
		if (email != null) body.put("email", email);
		return body;
	}

}
