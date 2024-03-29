package io.inprice.api.app.ticket.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
 * Tests the functionality of CommentController.update(TicketCommentDTO)
 * 
 * @author mdpinar
 * @since 2021-07-17
 */
@RunWith(JUnit4.class)
public class UpdateTest {

	private static final String SERVICE_ENDPOINT = "/ticket/comment";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
				.put("id", "1")
  			.put("ticketId", "1")
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
	public void Comment_not_found_WITH_null_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("id");

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Comment not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_update_this_data_WITH_closed_comment() {
		JSONObject user = TestWorkspaces.Premium_plan_and_two_extra_users.ADMIN();

		Cookies cookies = TestUtils.login(user);

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

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", comment.getLong("id"));
		body.put("ticketId", comment.getLong("ticketId"));
		
		JSONObject json = callTheService(user, comment);

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
		
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", comment.getLong("id"));
		body.put("ticketId", comment.getLong("ticketId"));

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(914, json.getInt("status"));
		assertEquals("Ticket is closed!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) to find other workspace's comment, admin is logged in
	 *	b) searches a specific comment
	 *  c) picks first comment
	 *  e) evil user logs in
	 *  f) tries to update other workspace's comment
	 */
	@Test
	public void Ticket_not_found_WITH_wrong_id() {
		//to find other workspace's comment, admin is logged in
		Cookies cookies = TestUtils.login(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN());

		//searches a specific comment
		JSONArray commentList = TestFinder.searchComments(cookies, "CRITICAL");
		assertNotNull(commentList);

		TestUtils.logout(cookies);
		
		//picks first comment
		JSONObject comment = commentList.getJSONObject(0);
		comment.put("body", "This is an altered comment by an evil user!");

		//evil user logs in
		cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.ADMIN());

		//tries to delete other workspace's comment
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(comment)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertEquals("Ticket not found!", json.getString("reason"));
	}

	@Test
	public void Ticket_id_cannot_be_empty_WITH_null_ticket_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("ticketId");

		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Ticket id cannot be empty!", json.getString("reason"));
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
	public void You_are_not_allowed_to_do_this_operation_WITH_viewer_but_not_the_creator() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());

		JSONArray commentList = TestFinder.searchComments(cookies, "HIGH");

		assertNotNull(commentList);
		assertEquals(2, commentList.length());

		//get the first comment
		JSONObject comment = commentList.getJSONObject(1); //attention pls! the first one is not editable!
		comment.put("level", "HIGH");
		comment.put("body", "This is an updated body!");

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS) //attention pls!
			.cookie(cookies)
			.body(comment)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.EDITOR());

		JSONArray commentList = TestFinder.searchComments(cookies, "HIGH");

		assertNotNull(commentList);
		assertEquals(2, commentList.length());

		//get the first comment
		JSONObject comment = commentList.getJSONObject(1); //attention pls!
		comment.put("level", "HIGH");
		comment.put("body", "This is an updated body!");

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(comment)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestWorkspaces.Standard_plan_and_no_extra_user.ADMIN(), body);
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
