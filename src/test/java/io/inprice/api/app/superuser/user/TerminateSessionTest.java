package io.inprice.api.app.superuser.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of superuser's User -> Controller.terminateSession(String hash)
 * 
 * @author mdpinar
 * @since 2021-07-24
 */
@RunWith(JUnit4.class)
public class TerminateSessionTest {

	private static final String SERVICE_ENDPOINT = "/sys/user/session/terminate/{hash}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("hash", "1")
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	/**
	 * Fulfills the same test for three types of users; viewer, editor and admin.
	 */
	@Test
	public void Forbidden_WITH_normal_users() {
		for (JSONObject user: Fixtures.NORMAL_USER_LIST) {
			JSONObject json = callTheService(user, "AB-123");

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}
	
	@Test
	public void User_not_found_WITH_non_existing_id() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, "AB-123");
		
		assertEquals(404, json.getInt("status"));
		assertEquals("User not found!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 * 	a) super user searches a specific user to get his userId
	 * 	b) normal user logs in
	 *	c) super user fetches session list of that user by his id
	 *	d) users log out and assertions start
	 *	e) super user tries to terminate the session of found hash
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject user = TestWorkspaces.Without_a_plan_and_extra_user.ADMIN();

		//super user searches a specific user to get his userId
		JSONArray userList = TestFinder.searchUsers(user.getString("email"));
		JSONObject foundUser = userList.getJSONObject(0);
		
		//normal user logs in
		Cookies userCookies = TestUtils.login(user);

		//super user fetches session list of that user by his id
		Cookies superCookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.get("/sys/user/details/sessions/{userId}")
			.cookie(superCookies)
			.routeParam("userId", ""+foundUser.getLong("id"))
			.asJson();

		//super user logs out
		TestUtils.logout(superCookies);
		
		JSONObject json = res.getBody().getObject();

		//and assertions start
		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
		
		JSONArray data = json.getJSONArray("data");
		JSONObject session = data.getJSONObject(0);

		//super user tries to terminate the session of found hash
		JSONObject result = callTheService(Fixtures.SUPER_USER, session.getString("hash"));

		//normal user logs out
		TestUtils.logout(userCookies);
		
		assertEquals(200, result.getInt("status"));
		assertEquals(0, result.getJSONArray("data").length());
	}

	private JSONObject callTheService(JSONObject user, String hash) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.cookie(cookies)
			.routeParam("hash", (hash != null ? hash : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
