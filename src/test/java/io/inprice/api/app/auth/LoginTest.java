package io.inprice.api.app.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.app.utils.Fixtures;
import io.inprice.api.app.utils.TestHelper;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * 
 * @author mdpinar
 * @since 2021-06-28
 */
@RunWith(JUnit4.class)
public class LoginTest {

	private static final String SERVICE_ENDPOINT = "/login";

	@BeforeClass
	public static void setup() {
		TestHelper.initTestServers();
	}

	@Test
	public void Request_body_is_invalid_WITH_no_body() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 400);
    assertEquals(json.getString("reason"), "Request body is invalid!");
	}

	@Test
	public void Email_address_cannot_be_empty_WITH_empty_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createUser(null, "1234"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 400);
    assertEquals(json.getString("reason"), "Email address cannot be empty!");
	}

	@Test
	public void Password_cannot_be_empty_WITH_empty_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createUser("admin@inprice.io", null))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 400);
    assertEquals(json.getString("reason"), "Password cannot be empty!");
	}

	@Test
	public void Invalid_email_address_WITH_wrong_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createUser("admininprice.io", "1234"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 400);
    assertEquals(json.getString("reason"), "Invalid email address!");
	}

	@Test
	public void Email_address_must_be_between_8_and_128_chars_WITH_shorter_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createUser("a@xy.io", "1234"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 400);
    assertEquals(json.getString("reason"), "Email address must be between 8 and 128 chars!");
	}

	@Test
	public void Email_address_must_be_between_8_and_128_chars_WITH_longer_email() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createUser(RandomStringUtils.randomAlphabetic(118)+"@inprice.io", "1234"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 400);
    assertEquals(json.getString("reason"), "Email address must be between 8 and 128 chars!");
	}

	@Test
	public void Password_length_must_be_between_4_and_16_chars_WITH_shorter_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createUser("admin@inprice.io", "123"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 400);
    assertEquals(json.getString("reason"), "Password length must be between 4 and 16 chars!");
	}

	@Test
	public void Password_length_must_be_between_4_and_16_chars_WITH_longer_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createUser("admin@inprice.io", RandomStringUtils.randomAlphabetic(17)))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 400);
    assertEquals(json.getString("reason"), "Password length must be between 4 and 16 chars!");
	}

	@Test
	public void Invalid_email_or_password_WITH_wrong_password() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(createUser("admin@inprice.io", "1235"))
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(json.getInt("status"), 113);
    assertEquals(json.getString("reason"), "Invalid email or password!");
	}

	@Test
	public void Banned_user_FOR_banned_user() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.BANNED_USER)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 403);
		assertEquals(json.getString("reason"), "Banned user!");
	}

	@Test
	public void Everything_must_be_OK_WITH_correct_credentials() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.ADMIN_USER)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 200);
    assertNotNull(json.get("data"));
	}

	@Test
	public void Everything_must_be_OK_WITH_correct_credentials_OF_superuser() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(Fixtures.SUPER_USER)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(json.getInt("status"), 200);
    assertNotNull(json.get("data"));
	}
	
	private JSONObject createUser(String email, String password) {
		JSONObject user = new JSONObject();
		if (email != null) user.put("email", email);
		if (password != null) user.put("password", password);
		return user;
	}

}
