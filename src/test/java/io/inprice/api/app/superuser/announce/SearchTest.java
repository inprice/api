package io.inprice.api.app.superuser.announce;

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
 * Tests the functionality of superuser's Announce -> Controller.search(SearchDTO)
 * 
 * @author mdpinar
 * @since 2021-07-23
 */
@RunWith(JUnit4.class)
public class SearchTest {

	private static final String SERVICE_ENDPOINT = "/sys/announces/search";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
  			.put("searchBy", "TITLE")
  			.put("types", new String[] { "USER", "ACCOUNT", "SYSTEM" })
  			.put("levels", new String[] { "INFO", "WARNING" })
	    	.put("startingAt", "2020-07-23 12:00:00")
	    	.put("endingAt", "2050-07-23 14:00:00")
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
	public void Everything_must_be_ok_FOR_account() {
		String[] types = new String[] { "SYSTEM", "ACCOUNT", "USER" };
		
		int announceCount = 0;

		//search per type (and cumulate the announce count to check if it equals to total announce count below
		for (String type: types) {
  		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
  		body.put("types", new String[] { type });
  
  		JSONObject json = callTheService(body);
  		
  		JSONObject data = json.getJSONObject("data");
  		JSONArray rows = data.getJSONArray("rows");
  		
  		assertEquals(200, json.getInt("status"));
  		assertTrue(rows.length() > 0);
  		
  		announceCount += rows.length();
		}
		
		//finding total announce count
		JSONObject json = callTheService(SAMPLE_BODY);
		JSONObject data = json.getJSONObject("data");

		JSONArray rows = data.getJSONArray("rows");
		assertEquals(announceCount, rows.length());
	}
	
	private JSONObject callTheService(JSONObject body) {
		return callTheService(Fixtures.SUPER_USER, body);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
