package io.inprice.api.app.ticket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
 * Tests the functionality of TicketController.findById(Long ticketId)
 * 
 * @author mdpinar
 * @since 2021-07-17
 */
@RunWith(JUnit4.class)
public class FindByIdTest {

	private static final String SERVICE_ENDPOINT = "/ticket/{id}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
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

	/**
	 * Consists of four steps;
	 *	a) to gather other workspace's tickets, admin is logged in
	 *	b) finds some specific tickets
	 *  c) picks one of them
	 *  d) evil user tries to find the ticket
	 */
	@Test
	public void Ticket_not_found_WHEN_trying_to_find_someone_elses_ticket() {
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
	public void You_must_bind_an_workspace_WITH_superuser_WITHOUT_binding_workspace() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an workspace!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) binds to a specific workspace
	 * 	c) gets ticket list (must not be empty)
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_workspace() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);
		
		JSONArray workspaceList = TestFinder.searchWorkspaces(cookies, "Without A Plan and Extra User");
		JSONObject workspace = workspaceList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put("/sys/workspace/bind/{workspaceId}")
			.cookie(cookies)
			.routeParam("workspaceId", ""+workspace.getLong("id"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "NORMAL" }, 0);
		JSONObject ticket = ticketList.getJSONObject(0);
		
		res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "LOW" }, 0);

		assertNotNull(ticketList);
		assertEquals(2, ticketList.length());

		//get the first ticket
		JSONObject ticket = ticketList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER());

		JSONArray ticketList = TestFinder.searchTickets(cookies, new String[] { "HIGH" }, 1); //for his viewer session!

		assertNotNull(ticketList);
		assertEquals(1, ticketList.length());

		//get the first ticket
		JSONObject ticket = ticketList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_1_HEADERS) //for his viewer session!
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	private JSONObject callTheService(Long id) {
		return callTheService(TestWorkspaces.Basic_plan_but_no_extra_user.ADMIN(), id);
	}

	private JSONObject callTheService(JSONObject user, Long id) {
		return callTheService(user, id, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long id, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.routeParam("id", (id != null ? id.toString() : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
