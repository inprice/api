package io.inprice.api.app.superuser.ticket.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
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
 * Tests the functionality of superuser's Ticket -> Comment -> Controller.insert(TicketCommentDTO)
 * 
 * @author mdpinar
 * @since 2021-07-24
 */
@RunWith(JUnit4.class)
public class InsertTest {

	private static final String SERVICE_ENDPOINT = "/sys/ticket/comment";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
				.put("ticketId", 1)
	    	.put("body", "This comment is added by super user.");

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
	public void Ticket_not_found_WITHOUT_ticket_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("ticketId");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Ticket not found!", json.getString("reason"));
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
    assertEquals("Body cannot be shorter than 12 chars!", json.getString("reason"));
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

	/**
	 * User first closes a ticket then tries to add a new comment
	 */
	@Test
	public void Ticket_is_closed() {
		//first call to close ticket
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		JSONObject ticketStatusChangeBody = new JSONObject();
		ticketStatusChangeBody.put("id", 2L);  //important!
		ticketStatusChangeBody.put("status", "CLOSED");

		HttpResponse<JsonNode> res = Unirest.put("/sys/ticket/change-status")
			.cookie(cookies)
			.body(ticketStatusChangeBody)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));

		//second call to add a new comment
		JSONObject commentBody = new JSONObject(SAMPLE_BODY.toMap());
		commentBody.put("ticketId", 2L); //important!

		json = callTheService(commentBody);

		assertEquals(914, json.getInt("status"));
		assertEquals("Ticket is closed!", json.getString("reason"));
	}

	/**
	 * Super user has an ability of changing ticket's status during adding a new comment
	 * 
	 * Consists of six steps;
	 * 	a) an admin logs in
	 * 	b) searches some certain tickets
	 * 	c) picks the first ticket
	 * 	d) prepares the body for both adding a comment and changing the status of the ticket
	 * 	e) the admin logs in again
	 * 	f) searches the changed ticket
	 */
	@Test
	public void Ticket_status_must_be_set_WITH_adding_comment() {
		final String TICKET_NEW_STATUS = "WAITING_FOR_VERSION";

		//an admin logs in
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		//searches some certain tickets
		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "LOW" }, 0);
		TestUtils.logout(cookies);

		assertNotNull(ticketList);
		assertEquals(2, ticketList.length());
		
		//picks the first ticket
		JSONObject ticket = ticketList.getJSONObject(0);

		//prepares the body for both adding a comment and changing the status of the ticket
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("ticketId", ticket.getLong("id")); //important!
		body.put("ticketNewStatus", TICKET_NEW_STATUS);

		JSONObject json = callTheService(body);

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		//the admin logs in again
		cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		//searches the changed ticket
		ticketList = TestFinder.searchTickets(cookies, new String[] { "LOW" }, 0);
		TestUtils.logout(cookies);
		
		for (int i = 0; i < ticketList.length(); i++) {
			JSONObject tckt = ticketList.getJSONObject(i);
			if (tckt.getLong("id") == ticket.getLong("id")) {
				ticket = tckt;
				break;
			}
		}

		assertEquals(200, json.getInt("status"));
		assertEquals(TICKET_NEW_STATUS, ticket.getString("status"));
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject json = callTheService(SAMPLE_BODY);

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
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
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();

		return res.getBody().getObject();
	}

}
