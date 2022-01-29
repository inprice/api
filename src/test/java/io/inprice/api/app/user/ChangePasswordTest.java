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
 * Tests the functionality of UserController.changePassword(PasswordDTO) 
 * 
 * @author mdpinar
 * @since 2021-07-11
 */
@RunWith(JUnit4.class)
public class ChangePasswordTest {

	private static final String SERVICE_ENDPOINT = "/user/change-password";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(createBody("1234-AB", "ABCD-10", "ABCD-10"))
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
	public void Password_cannot_be_empty() {
		JSONObject json = callTheServiceWith("1234-AB", null, "ABCD-11");

		assertEquals(400, json.getInt("status"));
    assertEquals("Password cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Password_length_must_be_between_6_and_16_chars_WITH_shorter_password() {
		JSONObject json = callTheServiceWith("1234-AB", "ABCDX");

		assertEquals(400, json.getInt("status"));
    assertEquals("Password length must be between 6 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Password_length_must_be_between_6_and_16_chars_WITH_longer_password() {
		JSONObject json = callTheServiceWith("1234-AB", RandomStringUtils.randomAlphabetic(17));

		assertEquals(400, json.getInt("status"));
    assertEquals("Password length must be between 6 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Passwords_are_mismatch_WITH_different_passwords() {
		JSONObject json = callTheServiceWith("1234-AB", "ABCD-10", "ABCD-11");

		assertEquals(400, json.getInt("status"));
    assertEquals("Passwords are mismatch!", json.getString("reason"));
	}

	@Test
	public void Old_password_cannot_be_empty() {
		JSONObject json = callTheServiceWith("", "ABCD-10", "ABCD-10");
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Old password cannot be empty!", json.get("reason"));
	}

	@Test
	public void Old_password_is_incorrect() {
		JSONObject json = callTheServiceWith("1234-AC", "ABCD-10", "ABCD-10");
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Old password is incorrect!", json.get("reason"));
	}

	@Test
	public void New_password_cannot_be_the_same_as_old_password() {
		JSONObject json = callTheServiceWith("1234-AB", "1234-AB", "1234-AB");
		
		assertEquals(400, json.getInt("status"));
		assertEquals("New password cannot be the same as old password!", json.get("reason"));
	}

	@Test
	public void Everything_must_be_ok() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Professional_plan_and_one_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(createBody("1234-AB", "';\"~.1", "';\"~.1"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.get("reason"));

		//we have to set old password again for the health of other tests
		res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(createBody("';\"~.1", "1234-AB", "1234-AB"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.get("reason"));

	}

	private JSONObject createBody(String oldPassword, String password, String repeatPassword) {
		JSONObject body = new JSONObject();
		if (oldPassword != null) body.put("oldPassword", oldPassword);
		if (password != null) body.put("password", password);
		if (repeatPassword != null) body.put("repeatPassword", repeatPassword);
		return body;
	}

	private JSONObject callTheServiceWith(String oldPassword, String password) {
		return callTheServiceWith(oldPassword, password, password);
	}
	
	private JSONObject callTheServiceWith(String oldPassword, String password, String repeatPassword) {
		//creating the body
		JSONObject body = createBody(oldPassword, password, repeatPassword);

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

}
