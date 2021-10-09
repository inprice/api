package io.inprice.api.app.superuser.link;

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
 * Tests the functionality of superuser's Link -> Controller.search(SearchDTO)
 * 
 * @author mdpinar
 * @since 2021-07-23
 */
@RunWith(JUnit4.class)
public class SearchTest {

	private static final String SERVICE_ENDPOINT = "/sys/links/search";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
  			.put("searchBy", "NAME")
  			.put("statuses", new String[] { "AVAILABLE", "NOT_AVAILABLE" })
  			.put("alarmStatus", "ALL")
  			.put("orderBy", "PLATFORM")
	    	;

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(SAMPLE_BODY)
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
			JSONObject json = callTheService(user, SAMPLE_BODY);

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, SAMPLE_BODY);
		assertTrue(json.has("data"));

		JSONArray rows = json.getJSONArray("data");
		assertEquals(25, rows.length());
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
