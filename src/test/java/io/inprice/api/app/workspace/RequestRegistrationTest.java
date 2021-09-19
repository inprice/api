package io.inprice.api.app.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of WorkspaceController.requestRegistration(RegisterDTO)
 * 
 * @author mdpinar
 * @since 2021-07-06
 */
@RunWith(JUnit4.class)
public class RequestRegistrationTest {

	private static final String SERVICE_ENDPOINT = "/request-registration";

	private static final JSONObject SAMPLE_BODY = 
		new JSONObject()
    	.put("workspaceName", "Acme X Inc.")
    	.put("password", "1234")
    	.put("repeatPassword", "1234");

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
	public void Workspace_name_cannot_be_empty_WITH_empty_workspace_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("workspaceName");
		body.put("email", "user-01@acme-x.com");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Workspace name cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Workspace_name_must_be_between_3_and_70_chars_WITH_shorter_email() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("workspaceName", "ab");
		body.put("email", "user-02@acme-x.com");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Workspace name must be between 3 - 70 chars!", json.getString("reason"));
	}

	@Test
	public void Workspace_name_must_be_between_3_and_70_chars_WITH_longer_email() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("workspaceName", RandomStringUtils.randomAlphabetic(71));
		body.put("email", "user-03@acme-x.com");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Workspace name must be between 3 - 70 chars!", json.getString("reason"));
	}

	@Test
	public void Email_address_cannot_be_empty_WITH_empty_email() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Invalid_email_address_WITH_wrong_email() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("email", "admininprice.io");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Invalid email address!", json.getString("reason"));
	}

	@Test
	public void Email_address_must_be_between_8_and_128_chars_WITH_shorter_email() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("email", "a@xy.io");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address must be between 8 - 128 chars!", json.getString("reason"));
	}

	@Test
	public void Email_address_must_be_between_8_and_128_chars_WITH_longer_email() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("email", RandomStringUtils.randomAlphabetic(118)+"@inprice.io");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address must be between 8 - 128 chars!", json.getString("reason"));
	}

	@Test
	public void Password_cannot_be_empty_WITH_empty_password() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("password");
		body.put("email", "user-04@acme-x.com");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Password cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Password_length_must_be_between_4_and_16_chars_WITH_shorter_password() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("password", "123");
		body.put("email", "user-05@acme-x.com");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
    assertEquals("Password length must be between 4 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Password_length_must_be_between_4_and_16_chars_WITH_longer_password() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("password", RandomStringUtils.randomAlphabetic(17));
		body.put("email", "user-06@acme-x.com");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Password length must be between 4 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Passwords_are_mismatch_WITH_different_passwords() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("repeatPassword", "1235");
		body.put("email", "user-07@acme-x.com");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
    assertEquals("Passwords are mismatch!", json.getString("reason"));
	}

	@Test
	public void Banned_user() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("email", Fixtures.BANNED_USER.get("email"));

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Banned user!", json.getString("reason"));
	}

	/**
	 * Satisfies two cases
	 * 	a) Already registered user
	 * 	b) Super user
	 */
	@Test
	public void Already_registered_user() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("email", Fixtures.SUPER_USER.get("email"));
		
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(872, json.getInt("status"));
		assertEquals("Already registered user! Signing up is an option for only newcomers! Please use 'Create Workspace' menu after login.", json.getString("reason"));
	}

	@Test
	public void This_email_is_already_requested_please_wait_some_time_to_try_again() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("email", "user-01@acme-x.com"); //requested above!

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(814, json.getInt("status"));
		assertEquals("This email is already requested, please wait some time to try again!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("email", "user-00@acme-x.com");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
	}

}
