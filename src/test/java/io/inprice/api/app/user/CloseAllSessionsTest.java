package io.inprice.api.app.user;

import static org.junit.Assert.assertEquals;

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
 * Tests the functionality of UserController.closeAllSessions()
 * 
 * @author mdpinar
 * @since 2021-07-11
 */
@RunWith(JUnit4.class)
public class CloseAllSessionsTest {

	private static final String SERVICE_ENDPOINT = "/user/close-all-sessions";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Forbidden_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 * 	a) User logs in
	 * 	b) close all his sessions
	 * 	c) logs out
	 * 	d) tries to close all the sessions again
	 */
	@Test
	public void Everything_must_ok() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Basic_plan_but_no_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));

		//tries to close the sessions again!
		res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		
		json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.getString("reason"));
	}

}
