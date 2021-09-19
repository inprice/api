package io.inprice.api.app.ticket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 * Tests the functionality of TicketController.update(TicketDTO)
 * 
 * @author mdpinar
 * @since 2021-07-16
 */
@RunWith(JUnit4.class)
public class UpdateTest {

	private static final String SERVICE_ENDPOINT = "/ticket";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
				.put("id", "1")
  			.put("priority", "LOW")
  			.put("type", "FEEDBACK")
	    	.put("subject", "SUBSCRIPTION")
	    	.put("body", "It would be awesome if we can pay via paypal!");

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
	public void Ticket_not_found_WITH_null_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("id");

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Ticket not found!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 *	a) to gather other workspace's tickets, admin is logged in
	 *	b) finds some specific tickets
	 *  c) picks one of them
	 *  d) evil user tries to update the ticket
	 */
	@Test
	public void Ticket_not_found_WHEN_trying_to_update_someone_elses_ticket() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "LOW" }, 0);
		TestUtils.logout(cookies); //here is important!
		
		assertNotNull(ticketList);
		JSONObject ticket = ticketList.getJSONObject(0);

		//evil user tries to update the ticket
		JSONObject json = callTheService(ticket);

		assertEquals(404, json.getInt("status"));
		assertEquals("Ticket not found!", json.getString("reason"));
	}

	@Test
	public void Priority_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("priority");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Priority cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Ticket_type_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("type");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Ticket type cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Subject_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("subject");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Subject cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Issue_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("body");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Issue cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Issue_must_be_between_12_and_1024_chars_WITH_shorter_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("body", RandomStringUtils.randomAlphabetic(11));

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Issue must be between 12 - 1024 chars!", json.getString("reason"));
	}

	@Test
	public void Issue_must_be_between_12_and_1024_chars_WITH_longer_body() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("body", RandomStringUtils.randomAlphabetic(1025));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Issue must be between 12 - 1024 chars!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, SAMPLE_BODY);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "HIGH" }, 1); //since he is a viewer!

		assertNotNull(ticketList);
		assertEquals(1, ticketList.length());

		//get the first alarm for a product
		JSONObject ticket = ticketList.getJSONObject(0);
		ticket.put("level", "HIGH");
		ticket.put("body", "This is an updated body!");

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_1_HEADERS) //attention pls!
			.cookie(cookies)
			.body(ticket)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestWorkspaces.Basic_plan_but_no_extra_user.ADMIN(), body);
	}

	private JSONObject callTheService(JSONObject user, JSONObject body) {
		return callTheService(user, body, 0);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
