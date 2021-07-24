package io.inprice.api.app.membership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestRoles;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of MembershipController.changeRole(InvitationUpdateDTO) 
 * 
 * @author mdpinar
 * @since 2021-07-06
 */
@RunWith(JUnit4.class)
public class ChangeRoleTest {

	private static final String SERVICE_ENDPOINT = "/membership/change-role";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(createBody(1l, TestRoles.VIEWER))
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertNotNull("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(1l, TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_editor_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.EDITOR());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(1l, TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	/**
	 * Consists of two steps;
	 *    a) get member list to find member id
	 *    b) change member's role
	 */
	@Test
	public void Role_must_be_either_EDITOR_or_VIEWER_WITH_empty_role() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//get member list to find member id
		Long memberId = findMemberIdByIndex(cookies, 0);

		//changes member's role
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(memberId, null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
    assertEquals("Role must be either EDITOR or VIEWER!", json.getString("reason"));
	}

	/**
	 * Consists of two steps;
	 *    a) get member list to find member id
	 *    b) change member's role
	 */
	@Test
	public void Role_must_be_either_EDITOR_or_VIEWER_WITH_ADMIN_role() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//get member list to find member id
		Long memberId = findMemberIdByIndex(cookies, 0);

		//changes member's role
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(memberId, TestRoles.ADMIN))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
    assertEquals("Role must be either EDITOR or VIEWER!", json.getString("reason"));
	}

	/**
	 * Consists of two steps;
	 *    a) get member list to find member id
	 *    b) change member's role
	 */
	@Test
	public void Role_must_be_either_EDITOR_or_VIEWER_WITH_SUPER_role() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//get member list to find member id
		Long memberId = findMemberIdByIndex(cookies, 0);

		//changes member's role
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(memberId, TestRoles.SUPER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
    assertEquals("Role must be either EDITOR or VIEWER!", json.getString("reason"));
	}

	/**
	 * Consists of two steps;
	 *    a) find a member
	 *    b) change member's role
	 */
	@Test
	public void Not_suitable_FOR_the_same_role() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//finding the first member
		JSONObject member = findMemberByIndex(cookies, 0);
		Long memberId = member.getLong("id");
		String role = member.getString("role");

		//changes member's role
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(memberId, TestRoles.valueOf(role)))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(610, json.getInt("status"));
    assertEquals("Not suitable!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 *    a) find the second member and delete him
	 *    b) delete him
	 *    c) try to change his role
	 */
	@Test
	public void This_member_is_already_deleted_FOR_a_deleted_member() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//finding the second member
		Long memberId = findMemberIdByIndex(cookies, 1); //attention pls!

		//deleting him
		HttpResponse<JsonNode> res = Unirest.delete("/membership/{id}")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", memberId.toString())
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		//better wait some time before second call!
		try {
			Thread.sleep(250);
		} catch (InterruptedException ignored) {}
		
		//try to change member's role
		res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(memberId, TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(802, json.getInt("status"));
    assertEquals("This member is already deleted!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(1l, TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(511, json.getInt("status"));
		assertNotNull("You are not allowed to do this operation!", json.get("reason"));
	}

	@Test
	public void Member_not_found_WITH_wrong_id() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(1l, TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(404, json.getInt("status"));
		assertNotNull("Member not found!", json.get("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) to get other account's members, admin is logged in
	 *	b) fetch all members
	 *  c) picks one of those members
	 *  d) evil user logs in
	 *  e) tries to change the role of other account's member
	 */
	@Test
	public void Member_not_found_WHEN_trying_to_update_the_role_of_a_foreigner() {
		//to gather other account's links, admin is logged in
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//searches some specific links
		JSONArray memberList = TestFinder.getMembers(cookies);
		TestUtils.logout(cookies); //here is important!

		assertNotNull(memberList);

		//picks one of those links
		JSONObject member = memberList.getJSONObject(0);

		//evil user logs in
		cookies = TestUtils.login(TestAccounts.Standard_plan_and_one_extra_user.ADMIN());

		//tries to change the role of other account's member
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(member.getLong("id"), TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertNotNull("Member not found!", json.get("reason"));
	}

	/**
	 * Consists of five steps;
	 *    a) super user logs in
	 *    b) searches user by email
	 *    c) finds user member_id by user id
	 *    d) admin logs in
	 *    e) tries to change his own role
	 */
	@Test
	public void Member_not_found_WHEN_an_admin_tries_to_change_his_own_role() {
		//super user logs in
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		//the user and his email
		JSONObject user = TestAccounts.Standard_plan_and_one_extra_user.ADMIN();
		String email = user.getString("email");

		//searches user by email
		HttpResponse<JsonNode> res = Unirest.post("/sys/users/search")
			.cookie(cookies)
			.body(new JSONObject()
					.put("term", email)
				)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");

		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		assertEquals(1, data.length());
		
		Long userId = data.getJSONObject(0).getLong("id");

		//find user member id by user id
		res = Unirest.get("/sys/user/details/memberships/{userId}")
			.cookie(cookies)
			.routeParam("userId", userId.toString())
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();
		data = json.getJSONArray("data");

		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		assertEquals(1, data.length());
		
		Long memberId = data.getJSONObject(0).getLong("id");
		
		cookies = TestUtils.login(user);
		
		//tries to change his role
		res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(memberId, TestRoles.EDITOR))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertEquals("Membership not found!", json.getString("reason"));
	}

	/**
	 * Consists of two steps;
	 *    a) find member id
	 *    b) change member's role
	 */
	@Test
	public void Everything_must_be_ok_WITH_admin_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//get member list to find member id
		Long memberId = findMemberIdByIndex(cookies, 0);

		//changes member's role
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(memberId, TestRoles.EDITOR))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private Long findMemberIdByIndex(Cookies cookies, int index) {
		return findMemberByIndex(cookies, index).getLong("id");
	}

	private JSONObject findMemberByIndex(Cookies cookies, int index) {
		HttpResponse<JsonNode> res = Unirest.get("/membership")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");
		
		return data.getJSONObject(index);
	}

	private JSONObject createBody(Long memberId, TestRoles role) {
		JSONObject body = new JSONObject();
		if (memberId != null) body.put("memberId", memberId);
		if (role != null) body.put("role", role.name());
		return body;
	}

}
