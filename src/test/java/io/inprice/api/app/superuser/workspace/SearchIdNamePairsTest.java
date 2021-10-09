package io.inprice.api.app.superuser.workspace;

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
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of ProductController.searchIdNameList(String byName)
 * 
 * @author mdpinar
 * @since 2021-07-22
 */
@RunWith(JUnit4.class)
public class SearchIdNamePairsTest {

	private static final String SERVICE_ENDPOINT = "/sys/workspace/id-name-pairs";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.queryString("term", "")
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
			JSONObject json = callTheService(user, "A");

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, "Without A Plan");
		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONArray data = json.getJSONArray("data");
		assertEquals(2, data.length());
	}
	
	private JSONObject callTheService(JSONObject user, String byName) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.cookie(cookies)
			.queryString("term", byName)
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
