package io.inprice.api.app.superuser.user;

import static org.junit.Assert.assertEquals;

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
 * Tests the functionality of superuser's User -> Controller.revokeBan(Long userId)
 * 
 * @author mdpinar
 * @since 2021-07-24
 */
@RunWith(JUnit4.class)
public class RevokeBanTest {

	private static final String SERVICE_ENDPOINT = "/sys/user/ban-revoke/{id}";

	private static final long SUPERUSER_ID = 1L;
	private static final long BANNED_USER_ID = 2L;

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

	@Test
	public void User_not_found_WITH_wrong_user_id() {
		JSONObject json = callTheService(0L);
		
		assertEquals(404, json.getInt("status"));
		assertEquals("User not found!", json.getString("reason"));
	}
	
	@Test
	public void You_cannot_revoke_your_ban() {
		JSONObject json = callTheService(SUPERUSER_ID);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("You cannot revoke your ban!", json.getString("reason"));
	}
	
	@Test
	public void User_is_not_banned() {
		JSONObject json = callTheService(3L);

		assertEquals(826, json.getInt("status"));
		assertEquals("User is not banned!", json.getString("reason"));
	}

	/**
	 * Fulfills the same test for three types of users; viewer, editor and admin.
	 */
	@Test
	public void Forbidden_WITH_normal_users() {
		for (JSONObject user: Fixtures.NORMAL_USER_LIST) {
			JSONObject json = callTheService(user, 3L);

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject json = callTheService(BANNED_USER_ID);

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private JSONObject callTheService(Long id) {
		return callTheService(Fixtures.SUPER_USER, id);
	}
	
	private JSONObject callTheService(JSONObject user, Long id) {
		Cookies cookies = TestUtils.login(user);

		JSONObject json = callTheService(cookies, id);
		TestUtils.logout(cookies);

		return json;
	}
	
	private JSONObject callTheService(Cookies cookies, Long id) {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+(id != null ? id : ""))
			.asJson();

		return res.getBody().getObject();
	}

}
