package io.inprice.api.app.superuser.ticket.comment;

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
 * Tests the functionality of superuser's Ticket -> Comment -> Controller.delete(Long commentId)
 * 
 * @author mdpinar
 * @since 2021-07-24
 */
@RunWith(JUnit4.class)
public class DeleteTest {

	private static final String SERVICE_ENDPOINT = "/sys/ticket/comment/{id}";

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
	public void Page_not_found_WITHOUT_id() {
		JSONObject json = callTheService(null);

		assertEquals(404, json.getInt("status"));
		assertEquals("Page not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_update_this_data_WITH_closed_comment() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN());

		JSONArray commentList = TestFinder.searchComments(cookies, "NORMAL");
		TestUtils.logout(cookies);

		assertNotNull(commentList);
		assertEquals(2, commentList.length());

		//get the first non editable comment
		JSONObject comment = null;
		for (int i = 0; i < commentList.length(); i++) {
			if (commentList.getJSONObject(i).getBoolean("editable") == false) {
				comment = commentList.getJSONObject(i);
				break;
			}
		}

		JSONObject json = callTheService(comment.getLong("id"));

		assertEquals(904, json.getInt("status"));
		assertEquals("You are not allowed to update this data!", json.getString("reason"));
	}

	/**
	 * Fulfills the same test for three types of users; viewer, editor and admin.
	 */
	@Test
	public void Forbidden_WITH_normal_users() {
		for (JSONObject user: Fixtures.NORMAL_USER_LIST) {
			JSONObject json = callTheService(user, 1L);

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}

	/**
	 * Consists of five steps;
	 * 	a) super user logs in
	 * 	b) changes the status of the ticket with 2 id
	 * 	c) fetches the changed ticket to get all the comments
	 * 	d) gets first comment
	 * 	e) tries to delete it
	 */
	@Test
	public void Ticket_is_closed() {
		//super user logs in
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		//changes the status of the ticket with 2 id
		JSONObject ticketStatusChangeBody = new JSONObject();
		ticketStatusChangeBody.put("id", 2L);  //important!
		ticketStatusChangeBody.put("status", "CLOSED");

		HttpResponse<JsonNode> res = Unirest.put("/sys/ticket/change-status")
			.cookie(cookies)
			.body(ticketStatusChangeBody)
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));

		//fetches the changed ticket to get all the comments
		res = Unirest.get("/sys/ticket/{id}")
			.cookie(cookies)
			.routeParam("id", "2")
			.asJson();
		TestUtils.logout(cookies);
		
		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));

		JSONObject data = json.getJSONObject("data");
		assertTrue(data.has("commentList"));
		
		//gets first comment
		JSONArray commentList = data.getJSONArray("commentList");
		JSONObject comment = commentList.getJSONObject(0);

		//tries to delete it
		json = callTheService(comment.getLong("id"));

		assertEquals(914, json.getInt("status"));
		assertEquals("Ticket is closed!", json.getString("reason"));
	}

	private JSONObject callTheService(Long id) {
		return callTheService(Fixtures.SUPER_USER, id);
	}

	private JSONObject callTheService(JSONObject user, Long id) {
		return callTheService(user, id, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long id, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.cookie(cookies)
			.routeParam("id", ""+(id != null ? id : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
