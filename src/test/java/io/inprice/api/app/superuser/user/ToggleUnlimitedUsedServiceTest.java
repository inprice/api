package io.inprice.api.app.superuser.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of superuser's User -> Controller.toggleUnlimitedUsedService(Long id)
 * 
 * @author mdpinar
 * @since 2021-07-24
 */
@RunWith(JUnit4.class)
public class ToggleUnlimitedUsedServiceTest {

	private static final String SERVICE_ENDPOINT = "/sys/user/used-service/toggle/{id}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("id", "1")
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
	public void Used_service_not_found_WITH_non_existing_id() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 999L);
		
		assertEquals(404, json.getInt("status"));
		assertEquals("Used service not found!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 * 	a) super user searches a specific user to get his userId
	 *	b) fetches used service list of the user
	 *	c) tries to toggle used service for the user
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		//super user searches a specific user to get his userId
		JSONArray userList = TestFinder.searchUsers(TestAccounts.Second_without_a_plan_and_extra_user.ADMIN().getString("email"));
		JSONObject foundUser = userList.getJSONObject(0);
		
		//fetches used service list of the foundUser
		Cookies superCookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.get("/sys/user/used-services/{userId}")
			.cookie(superCookies)
			.routeParam("userId", ""+foundUser.getLong("id"))
			.asJson();
		TestUtils.logout(superCookies);
		
		JSONObject json = res.getBody().getObject();

		//and assertions start
		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
		
		JSONArray data = json.getJSONArray("data");
		JSONObject firstStateOfUsedService = data.getJSONObject(0);

		//tries to toggle used service for the user
		JSONObject result = callTheService(Fixtures.SUPER_USER, firstStateOfUsedService.getLong("id"));

		assertEquals(200, result.getInt("status"));
		assertTrue(result.has("data"));

		data = result.getJSONArray("data");
		
		assertEquals(1, data.length());
		JSONObject lastStateOfUsedService = data.getJSONObject(0);

		assertNotEquals(firstStateOfUsedService.getBoolean("whitelisted"), lastStateOfUsedService.getBoolean("whitelisted"));
	}

	private JSONObject callTheService(JSONObject user, Long id) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.cookie(cookies)
			.routeParam("id", ""+(id != null ? id : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
