package io.inprice.api.app.superuser.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of superuser's User -> Controller.fetchDetails(Long userId)
 * 
 * @author mdpinar
 * @since 2021-07-24
 */
@RunWith(JUnit4.class)
public class FetchDetailsTest {

	private static final String SERVICE_ENDPOINT = "/sys/user/details/{userId}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("userId", "1")
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
			JSONObject json = callTheService(user, 1L);

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}
	
	@Test
	public void User_not_found_WITH_non_existing_id() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 999L);
		
		assertEquals(404, json.getInt("status"));
		assertEquals("User not found!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 3L);

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONObject data = json.getJSONObject("data");

		assertTrue(data.has("user"));
		assertTrue(data.has("membershipList"));
		assertTrue(data.has("sessionList"));
		assertTrue(data.has("usedServiceList"));
	}

	private JSONObject callTheService(JSONObject user, Long userId) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.cookie(cookies)
			.routeParam("userId", (userId != null ? ""+userId : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
