package io.inprice.api.app.ticket.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
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
 * Tests the functionality of CommentController.insert(TicketCommentDTO)
 * 
 * @author mdpinar
 * @since 2021-07-16
 */
@RunWith(JUnit4.class)
public class InsertTest {

	private static final String SERVICE_ENDPOINT = "/ticket/comment";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
	    	.put("body", "What dou you mean with this line?");

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

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Ticket_id_cannot_be_empty() {
		JSONObject json = callTheService(SAMPLE_BODY);

		assertEquals(400, json.getInt("status"));
		assertEquals("Ticket id cannot be empty!", json.getString("reason"));
	}
	
	@Test
	public void Ticket_not_found_WITH_invalid_ticket_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("ticketId", 999);

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Ticket not found!", json.getString("reason"));
	}

	@Test
	public void Body_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("body");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Body cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Body_must_be_between_12_and_1024_chars_WITH_shorter_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("body", RandomStringUtils.randomAlphabetic(11));

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Body must be between 12 - 1024 chars!", json.getString("reason"));
	}

	@Test
	public void Body_must_be_between_12_and_1024_chars_WITH_longer_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("body", RandomStringUtils.randomAlphabetic(1025));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Body must be between 12 - 1024 chars!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, SAMPLE_BODY);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Ticket_is_closed() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());
		
		// there must be only one ticket in
		// 11_workspace_with_standard_plan_and_two_extra_users.sql
		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "LOW" }, 0);
		JSONObject ticket = ticketList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("ticketId", ""+ticket.getLong("id"));

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(914, json.getInt("status"));
		assertEquals("Ticket is closed!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());
		
		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "HIGH" }, 0);
		JSONObject ticket = ticketList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("ticketId", ""+ticket.getLong("id"));

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestWorkspaces.Premium_plan_with_no_user.ADMIN(), body);
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
