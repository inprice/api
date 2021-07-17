package io.inprice.api.app.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.BeforeParam;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of acceptInvitation(LongDTO), leaveMembership(LongDTO) and rejectInvitation(LongDTO) in UserService
 * 
 * This class is equipped with Parameterized runner so that we are able to run the same tests for accepting, leaving and rejecting invitation! 
 * 
 * @author mdpinar
 * @since 2021-07-11
 */
@RunWith(Parameterized.class)
public class AcceptLeaveAndRejectInvitationTests {

	private String SERVICE_ENDPOINT = "/user";
	private String endpointPostfix;

	/**
	 * This method runs this class twice; for both acceptInvitation and rejectInvitation
	 * 
	 */
  @Parameterized.Parameters
  public static Object[][] getHttpMethodParams() {
  	return new Object[][] { { "/accept-invitation" }, { "/reject-invitation" }, { "/leave-membership" } };
  }
  
  public AcceptLeaveAndRejectInvitationTests(String postfix) {
  	this.SERVICE_ENDPOINT += postfix;
  	this.endpointPostfix = postfix;
  }

	@BeforeParam
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Forbidden_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT).asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Membership_not_found_WITH_membership_id() {
		JSONObject json = callTheService(TestAccounts.Premium_plan_and_three_pending_users.ADMIN(), 1L);

		assertEquals(404, json.getInt("status"));
		assertEquals("Membership not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 * 	a) admin user logs in and find first pending member and then logs out
	 * 	b) pending member found in the first step logs in
	 * 	c) accept / reject the invitation
	 * 	d) logs out
	 */
	@Test
	public void Everything_must_ok() {
		Long memberId = findMemberIdByStatus(endpointPostfix.equals("/leave-membership") ? "JOINED" : "PENDING");

		assertNotNull(endpointPostfix, memberId);

		Cookies cookies = TestUtils.login(Fixtures.USER_HAVING_THREE_MEMBERSHIPS);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(
  				new JSONObject().put("value", memberId)
  			)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.get("status"));
		assertEquals("OK", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 * 	a) admin user logs in and find first pending member and then logs out
	 * 	b) another user logs in
	 * 	c) tries to accept / reject the invitation
	 * 	d) logs out
	 */
	@Test
	public void Membership_not_found_WITH_evil_user() {
		//real membership (in this case, member's existence does matter but not his status!)
		Long memberId = findMemberIdByStatus(endpointPostfix.equals("/leave-membership") ? "PENDING" : "JOINED");

		assertNotNull(endpointPostfix, memberId);

		//must not have a permission to change the member's role!
		Cookies cookies = TestUtils.login(TestAccounts.Second_without_a_plan_and_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(
  				new JSONObject().put("value", memberId)
  			)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertEquals("Membership not found!", json.getString("reason"));
	}

	private JSONObject callTheService(JSONObject user, Long value) {
		JSONObject body = new JSONObject();
		if (value != null) body.put("value", value);
		
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);
		
		return res.getBody().getObject();
	}

	private Long findMemberIdByStatus(String wantedUserStatus) {
		Cookies cookies = TestUtils.login(Fixtures.USER_HAVING_THREE_MEMBERSHIPS);

		HttpResponse<JsonNode> res = Unirest.get("/user/memberships")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.get("status"));
		assertNotNull(json.getJSONArray("data"));

		JSONArray data = json.getJSONArray("data");

		for (int i=0; i<data.length(); i++) {
			JSONObject membership = data.getJSONObject(i);
			if (wantedUserStatus.equals(membership.getString("status"))) {
				return membership.getLong("id");
			}
		}
		return null;
	}

}
