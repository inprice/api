package io.inprice.api.app.ticket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 * Tests the functionality of TicketController.toggleSeenValue(Long ticketId)
 * 
 * @author mdpinar
 * @since 2021-07-17
 */
@RunWith(JUnit4.class)
public class ToggleSeenValueTest {

	private static final String SERVICE_ENDPOINT = "/ticket/seen/{id}";

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
	public void Page_not_found_WITH_null_id() {
		JSONObject json = callTheService(null);

		assertEquals(404, json.getInt("status"));
		assertEquals("Page not found!", json.getString("reason"));
	}

	@Test
	public void Ticket_not_found_WITH_wrong_id() {
		JSONObject json = callTheService(-71L);

		assertEquals(404, json.getInt("status"));
		assertEquals("Ticket not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "HIGH" }, 1); //since he is a viewer!

		assertNotNull(ticketList);
		assertEquals(1, ticketList.length());

		//get the first alarm for a group
		JSONObject ticket = ticketList.getJSONObject(0);
		ticket.put("level", "HIGH");
		ticket.put("body", "This is an updated body!");

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_1_HEADERS) //attention pls!
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	public JSONObject callTheService(Long id) {
		return callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), id);
	}

	public JSONObject callTheService(JSONObject user, Long id) {
		return callTheService(user, id, 0);
	}
	
	public JSONObject callTheService(JSONObject user, Long id, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.routeParam("id", (id != null ? ""+id : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
