package io.inprice.api.app.superuser.ticket;

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
 * Tests the functionality of superuser's Ticket -> Controller.changeStatus(ChangeStatusDTO)
 * 
 * @author mdpinar
 * @since 2021-07-24
 */
@RunWith(JUnit4.class)
public class ChangeStatusTest {

	private static final String SERVICE_ENDPOINT = "/sys/ticket/change-status";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
  			.put("id", 1L)
  			.put("status", "IN_PROGRESS")
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
	public void Invalid_status_FOR_empty_status() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("status");
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Invalid status!", json.getString("reason"));
	}
	
	@Test
	public void Ticket_not_found_WITHOUT_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("id");

		JSONObject json = callTheService(body);
		
		assertEquals(404, json.getInt("status"));
		assertEquals("Ticket not found!", json.getString("reason"));
	}
	
	@Test
	public void Ticket_not_found_WITH_wrong_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", 999L);
		
		JSONObject json = callTheService(body);
		
		assertEquals(404, json.getInt("status"));
		assertEquals("Ticket not found!", json.getString("reason"));
	}

	/**
	 * Makes the same service call twice to catch the case
	 */
	@Test
	public void Ticket_already_in_IN_PROGRESS_status() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());

		//first call
		JSONObject json = callTheService(body);

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));

		//second call
		json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Ticket is already in IN_PROGRESS status!", json.getString("reason"));
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
		body.put("id", 2L);

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
