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
 * Tests the functionality of superuser's User -> Controller.searchForAccessLog(ALSearchDTO)
 * 
 * @author mdpinar
 * @since 2021-07-24
 */
@RunWith(JUnit4.class)
public class SearchForAccessLogTest {

	private static final String SERVICE_ENDPOINT = "/sys/user/search-logs";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(new JSONObject())
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
			JSONObject json = callTheService(user, new JSONObject());

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}
	
	@Test
	public void Bad_request_WITHOUT_userId() {
		JSONObject body = new JSONObject();
		body.put("method", "POST");
		
		JSONObject json = callTheService(Fixtures.SUPER_USER, body);
		assertEquals(400, json.getInt("status"));
		assertEquals("Bad request!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject body = new JSONObject();
		body.put("userId", 1);
		body.put("method", "POST");

		JSONObject json = callTheService(Fixtures.SUPER_USER, body);
		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
