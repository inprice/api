package io.inprice.api.app.ticket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 * Tests the functionality of TicketController.delete(Long ticketId)
 * 
 * @author mdpinar
 * @since 2021-07-16
 */
@RunWith(JUnit4.class)
public class DeleteTest {

	private static final String SERVICE_ENDPOINT = "/ticket/{id}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
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
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 *	a) to gather other workspace's tickets, admin is logged in
	 *	b) finds some specific tickets
	 *  c) picks one of them
	 *  d) evil user tries to delete the ticket
	 */
	@Test
	public void Ticket_not_found_WHEN_trying_to_delete_someone_elses_ticket() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "LOW" }, 0);
		TestUtils.logout(cookies); //here is important!
		
		assertNotNull(ticketList);
		JSONObject ticket = ticketList.getJSONObject(0);

		//evil user tries to delete the ticket
		JSONObject json = callTheService(ticket.getLong("id"));

		assertEquals(404, json.getInt("status"));
		assertEquals("Ticket not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_update_this_operation_WITH_editor_but_not_the_creator() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_one_extra_user.EDITOR());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "LOW" }, 1);

		assertNotNull(ticketList);
		assertEquals(1, ticketList.length());

		//get the first ticket
		JSONObject ticket = ticketList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_update_this_data_FOR_IN_PROGRESS_ticket() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.EDITOR());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "LOW" }, 0);

		assertNotNull(ticketList);
		assertEquals(1, ticketList.length());

		//get the first ticket
		JSONObject ticket = ticketList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(904, json.getInt("status"));
		assertEquals("You are not allowed to update this data!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_link_WHEN_admin_tries_someone_elses_ticket() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "LOW" }, 0);

		assertNotNull(ticketList);
		assertEquals(2, ticketList.length());

		//get the first ticket
		JSONObject ticket = ticketList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "HIGH" }, 0); //for his viewer session!

		assertNotNull(ticketList);
		assertEquals(1, ticketList.length());

		//get the first ticket
		JSONObject ticket = ticketList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS) //for his viewer session!
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private JSONObject callTheService(Long id) {
		return callTheService(TestWorkspaces.Basic_plan_but_no_extra_user.ADMIN(), id);
	}

	private JSONObject callTheService(JSONObject user, Long id) {
		return callTheService(user, id, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long id, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.routeParam("id", (id != null ? id.toString() : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
