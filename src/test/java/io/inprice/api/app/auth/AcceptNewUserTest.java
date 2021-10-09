package io.inprice.api.app.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestRoles;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of AuthController.acceptNewUser(InvitationAcceptDTO, String timezone)
 * 
 * Out of scope:
 * 	- No need to check passwords again here since it is already done in Login and ForgotPassword test classes
 *  
 * @author mdpinar
 * @since 2021-07-01
 * 
 */
@RunWith(JUnit4.class)
public class AcceptNewUserTest {

	private static final String SERVICE_ENDPOINT = "/accept-invitation";

	private static final JSONObject SAMPLE_BODY = 
		new JSONObject()
			.put("fullName", "John Doe")
			.put("token", "XYZ-123")
    	.put("password", "1234-AB")
    	.put("repeatPassword", "1234-AB");

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
	public void Full_Name_cannot_be_empty_WITH_empty_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("fullName");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Full Name cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Full_Name_must_be_between_3_and_70_chars_WITH_shorter_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("fullName", "ab");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Full Name must be between 3 - 70 chars!", json.getString("reason"));
	}

	@Test
	public void Full_Name_must_be_between_3_and_70_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("fullName", RandomStringUtils.randomAlphabetic(71));

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Full Name must be between 3 - 70 chars!", json.getString("reason"));
	}

	@Test
	public void Invalid_token_WITH_empty_token() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("token");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Invalid token!", json.getString("reason"));
	}

	@Test
	public void Invalid_token_WITH_wrong_token() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("token", "XYZ-123");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(130, json.getInt("status"));
    assertEquals("Invalid token!", json.getString("reason"));
	}

	@Test
	public void Password_cannot_be_empty_WITH_empty_password() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("password");

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Password cannot be empty!", json.getString("reason"));
	}

	/**
	 * The steps are  
	 * 		1- Admin of S invites a non-existing user!
	 * 		2- Accepting his/her invitation
	 */
	@Test
	public void Everything_must_be_OK_WITH_new_user_invitation() {
		/* -------------------------------- 
		   Inviting a non-existing user
		 -------------------------------- */
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_three_pending_users.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post("/membership")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createInvitationBody(Fixtures.NON_EXISTING_EMAIL_1, TestRoles.EDITOR))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));

		JSONObject data = json.getJSONObject("data");
		assertNotNull(data.getString("token"));

		/* -------------------------------- 
	   Accepting him/her
  	 -------------------------------- */
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("token", data.getString("token"));

		res = Unirest.post(SERVICE_ENDPOINT)
			.body(body)
			.asJson();
		//new users can have session cookies. we need to log new user out here for the sake of other test cases!
		TestUtils.logout(res.getCookies());

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
	}

	private JSONObject createInvitationBody(String email, TestRoles role) {
		JSONObject body = new JSONObject();
		if (email != null) body.put("email", email);
		if (role != null) body.put("role", role.name());
		return body;
	}

}
