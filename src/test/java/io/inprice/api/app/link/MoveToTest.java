package io.inprice.api.app.link;

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
 * Tests the functionality of LinkController.moveTo(LinkMoveDTO)
 * 
 * @author mdpinar
 * @since 2021-07-19
 */
@RunWith(JUnit4.class)
public class MoveToTest {

	private static final String SERVICE_ENDPOINT = "/link/move";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(new JSONObject())
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(null, null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Group_not_found_WITHOUT_toGroup() {
		JSONObject json = callTheService(null, new Long[] { 1L });

		assertEquals(404, json.getInt("status"));
		assertEquals("Group not found!", json.getString("reason"));
	}

	@Test
	public void Link_not_found_WITHOUT_link_id_set() {
		JSONObject json = callTheService(1L, null);

		assertEquals(404, json.getInt("status"));
		assertEquals("Link not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L, new Long[] { 1L });

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), 1L, new Long[] { 1L }, 1); //attention!

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	/**
	 * Consists of six steps;
	 *	a) to gather other account's links, admin is logged in
	 *	b) searches some specific links
	 *  c) picks one of those links
	 *  d) builds body up
	 *  e) evil user logs in
	 *  f) tries to move other account's links
	 */
	@Test
	public void Link_not_found_WHEN_trying_to_move_someone_elses_links() {
		//to gather other account's links, admin is logged in
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//searches some specific links
		JSONArray linkList = TestFinder.searchLinks(cookies, "ACTIVE");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(linkList);

		//picks one of those links
		JSONObject link = linkList.getJSONObject(0);
		Long[] linkIds = { link.getLong("id") };
		Long toGroupId = findToGroupId(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), "Group 2 of Account-B");

		//evil user logs in
		cookies = TestUtils.login(TestAccounts.Standard_plan_and_one_extra_user.EDITOR());

		//builds the body up
		JSONObject body = new JSONObject();
		body.put("toGroupId", toGroupId);
		body.put("linkIdSet", linkIds);

		//tries to move other account's links
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertEquals("Link not found!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) an admin logs in
	 *	b) searches some specific links
	 *  c) pick one of them
	 *  d) builds body up with a new group name
	 *  e) tries to create new group by name and moves those links under it 
	 */
	@Test
	public void You_already_have_a_group_having_the_same_name_WHEN_creating_a_new_group() {
		//user logs in
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//searches some specific links
		JSONArray linkList = TestFinder.searchLinks(cookies, "TRYING");
		TestUtils.logout(cookies);

		assertNotNull(linkList);
		assertEquals(1, linkList.length());

		//builds the body up
		JSONObject body = new JSONObject();
		body.put("toGroupName", "Group 1 OF ACCOUNT-B");
		body.put("linkIdSet", new Long[] { linkList.getJSONObject(0).getLong("id") });

		cookies = TestUtils.login(TestAccounts.Basic_plan_but_no_extra_user.ADMIN());

		//moves those selected links
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(875, json.getInt("status"));
		assertEquals("You already have a group having the same name!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) editor or admin logs in
	 *	b) searches some specific links
	 *  c) gathers two of them
	 *  d) builds body up
	 *  e) deletes those selected links
	 */
	@Test
	public void Everything_must_be_ok_FOR_new_toGroupName() {
		//a user logs in
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_one_extra_user.EDITOR());

		//searches some specific links
		JSONArray linkList = TestFinder.searchLinks(cookies, "TRYING");

		assertNotNull(linkList);

		//gathers two of them
		Long[] linkIds = new Long[2];
		
		for (int i = 0; i < linkList.length(); i++) {
			JSONObject link = linkList.getJSONObject(i);
			linkIds[i] = link.getLong("id");
		}

		//builds the body up
		JSONObject body = new JSONObject();
		body.put("toGroupName", "This is a new group to move.");
		body.put("linkIdSet", linkIds);
		
		//moves those selected links
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) editor or admin logs in
	 *	b) searches some specific links
	 *  c) gathers two of them
	 *  d) builds body up
	 *  e) deletes those selected links
	 */
	@Test
	public void Everything_must_be_ok_FOR_admin() {
		Long toGroupId = findToGroupId(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), "Group 1 of Account-B");

		//user logs in
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//searches some specific links
		JSONArray linkList = TestFinder.searchLinks(cookies, "PROBLEM");

		assertNotNull(linkList);
		assertEquals(2, linkList.length());

		//gathers two of them
		Long[] linkIds = new Long[2];
		
		for (int i = 0; i < linkList.length(); i++) {
			JSONObject link = linkList.getJSONObject(i);
			linkIds[i] = link.getLong("id");
		}

		//builds the body up
		JSONObject body = new JSONObject();
		body.put("toGroupId", toGroupId);
		body.put("linkIdSet", linkIds);

		//moves those selected links
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private Long findToGroupId(JSONObject user, String groupName) {
		Cookies cookies = TestUtils.login(user);

		JSONArray groups = TestFinder.searchGroups(cookies, groupName);
		TestUtils.logout(cookies);

		assertNotNull(groups);

		return groups.getJSONObject(0).getLong("id");
	}

	private JSONObject callTheService(Long toGroupId, Long[] linkIds) {
		return callTheService(TestAccounts.Standard_plan_and_no_extra_users.ADMIN(), toGroupId, linkIds);
	}

	private JSONObject callTheService(JSONObject user, Long toGroupId, Long[] linkIds) {
		return callTheService(user, toGroupId, linkIds, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long toGroupId, Long[] linkIds, int session) {
		Cookies cookies = TestUtils.login(user);
		
		JSONObject body = null;
		if (toGroupId != null || linkIds != null) {
			body = new JSONObject();

			if (toGroupId != null) body.put("toGroupId", toGroupId);
			if (linkIds != null && linkIds.length > 0) body.put("linkIdSet", linkIds);
		}

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
