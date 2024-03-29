package io.inprice.api.app.ticket.comment;

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
 * Tests the functionality of CommentController.delete(Long commentId)
 * 
 * @author mdpinar
 * @since 2021-07-17
 */
@RunWith(JUnit4.class)
public class DeleteTest {

	private static final String SERVICE_ENDPOINT = "/ticket/comment/{id}";

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
	public void Request_body_is_invalid_WITH_null_id() {
		JSONObject json = callTheService(null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) to find other workspace's comment, admin is logged in
	 *	b) searches a specific comment
	 *  c) picks first comment
	 *  e) evil user logs in
	 *  f) tries to delete other workspace's comment
	 */
	@Test
	public void Comment_not_found_WITH_wrong_id() {
		//to find other workspace's comment, admin is logged in
		Cookies cookies = TestUtils.login(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN());

		//searches a specific comment
		JSONArray commentList = TestFinder.searchComments(cookies, "CRITICAL");
		assertNotNull(commentList);

		TestUtils.logout(cookies);
		
		//picks first comment
		JSONObject comment = commentList.getJSONObject(0);

		//evil user logs in
		cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.ADMIN());

		//tries to delete other workspace's comment
		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+comment.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertEquals("Comment not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_viewer_but_not_the_creator() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());

		JSONArray commentList = TestFinder.searchComments(cookies, "HIGH");

		assertNotNull(commentList);
		assertEquals(2, commentList.length());

		//get the first editable comment
		JSONObject comment = null;
		for (int i = 0; i < commentList.length(); i++) {
			if (commentList.getJSONObject(i).getBoolean("editable") == true) {
				comment = commentList.getJSONObject(i);
				break;
			}
		}

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS) //attention pls!
			.cookie(cookies)
			.routeParam("id", ""+comment.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_update_this_data_WITH_closed_comment() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());

		JSONArray commentList = TestFinder.searchComments(cookies, "NORMAL");

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

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS) //attention pls!
			.cookie(cookies)
			.routeParam("id", ""+comment.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(904, json.getInt("status"));
		assertEquals("You are not allowed to update this data!", json.getString("reason"));
	}

	@Test
	public void Ticket_is_closed() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.ADMIN());

		JSONArray commentList = TestFinder.searchComments(cookies, "LOW");

		assertNotNull(commentList);
		assertEquals(1, commentList.length());

		// get first comment
		JSONObject comment = commentList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+comment.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(914, json.getInt("status"));
		assertEquals("Ticket is closed!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_link_WHEN_admin_tries_to_someone_elses_comment() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN());

		JSONArray commentList = TestFinder.searchComments(cookies, "NORMAL");

		assertNotNull(commentList);
		assertEquals(2, commentList.length());

		//get the second comment which is editable
		JSONObject comment = commentList.getJSONObject(1);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+comment.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());

		JSONArray commentList = TestFinder.searchComments(cookies, "NORMAL");

		assertNotNull(commentList);
		assertEquals(2, commentList.length());

		//get the second comment which is editable
		JSONObject comment = commentList.getJSONObject(1);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+comment.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	private JSONObject callTheService(Long id) {
		return callTheService(TestWorkspaces.Standard_plan_and_no_extra_user.ADMIN(), id);
	}

	private JSONObject callTheService(JSONObject user, Long id) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", (id != null ? id.toString() : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
