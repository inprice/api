package io.inprice.api.app.superuser.link;

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
 * Tests the functionality of superuser's Link -> Controller.changeStatus(BulkChangetDTO)
 * 
 * @author mdpinar
 * @since 2021-07-23
 */
@RunWith(JUnit4.class)
public class ChangeStatusTest {

	private static final String SERVICE_ENDPOINT = "/sys/link/change-status";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
  			.put("status", "PAUSED")
  			.put("idSet", new Long[] { 1L, 2L })
  			;

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(SAMPLE_BODY)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}
	
	@Test
	public void Acceptable_statuses_PAUSED_RESOLVED_and_NOT_SUITABLE_WITHOUT_status() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("status");
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Acceptable statuses: PAUSED, RESOLVED and NOT_SUITABLE!", json.getString("reason"));
	}

	@Test
	public void Acceptable_statuses_PAUSED_RESOLVED_and_NOT_SUITABLE_WITH_wrong_status() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("status", "AVAILABLE");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Acceptable statuses: PAUSED, RESOLVED and NOT_SUITABLE!", json.getString("reason"));
	}
	
	@Test
	public void Link_not_found_WITHOUT_idSet() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("idSet");
		
		JSONObject json = callTheService(body);
		
		assertEquals(404, json.getInt("status"));
		assertEquals("Link not found!", json.getString("reason"));
	}
	
	@Test
	public void Link_not_found_WITH_wrong_ids() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("idSet", new Long[] { 888L, 999L });
		
		JSONObject json = callTheService(body);
		
		assertEquals(404, json.getInt("status"));
		assertEquals("Link not found!", json.getString("reason"));
	}

	@Test
	public void Link_not_found_FOR_the_links_RESOLVED_and_but_not_TOBE_IMPLEMENTED() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("status", "RESOLVED");
		body.put("idSet", new Long[] { 1L });

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Link not found!", json.getString("reason"));
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
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("idSet", new Long[] { 1L });

		JSONObject json = callTheService(body);

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(Fixtures.SUPER_USER, body);
	}

	private JSONObject callTheService(JSONObject user, JSONObject body) {
		Cookies cookies = TestUtils.login(user);

		JSONObject json = callTheService(cookies, body);
		TestUtils.logout(cookies);

		return json;
	}

	private JSONObject callTheService(Cookies cookies, JSONObject body) {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();

		return res.getBody().getObject();
	}

}
