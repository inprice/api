package io.inprice.api.app.membership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestRoles;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of MembershipService.resend(Long memberId)
 * 
 * Out of scope:
 * 	- Unable to test the case of "You cannot re-send invitation to yourself" since we cannot get the member id of an ADMIN
 * 
 * @author mdpinar
 * @since 2021-07-04
 */
@RunWith(JUnit4.class)
public class ResendTest {

	private static final String SERVICE_ENDPOINT = "/membership/{id}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("id", "1")
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertNotNull("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", "1")
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_editor_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.EDITOR());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", "1")
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Member_not_found_WITH_wrong_id() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", "1")
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(404, json.getInt("status"));
		assertNotNull("Member not found!", json.get("reason"));
	}

	@Test
	public void You_cannot_re_send_an_invitation_since_this_user_is_not_in_PENDING_status_FOR_joined_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());
		
		Long memberId = findMemberIdByIndex(cookies, 0);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", memberId.toString())
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
		assertEquals("You cannot re-send an invitation since this user is not in PENDING status!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 *    a) invites a non-existing user
	 *    b) get member list to find member id
	 *    b) re-sends the invitation for extra three times
	 */
	@Test
	public void You_can_re_send_invitation_for_the_same_user_up_to_three_times_FOR_the_same_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_one_extra_user.ADMIN());
		
		//invites a non-existing user
		HttpResponse<JsonNode> res = Unirest.post("/membership")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(new JSONObject()
  				.put("role", TestRoles.EDITOR)
  				.put("email", Fixtures.NON_EXISTING_EMAIL_2)
  			)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));
		
		//get member list to find member id
		Long memberId = findMemberIdByEmail(cookies, Fixtures.NON_EXISTING_EMAIL_2);

		//re-sends the invitation for extra three times
		for (int i = 0; i < 3; i++) {
  		res = Unirest.post(SERVICE_ENDPOINT)
  			.headers(Fixtures.SESSION_0_HEADERS)
  			.cookie(cookies)
  			.routeParam("id", memberId.toString())
  			.asJson();
		}
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
		assertEquals("You can re-send invitation for the same user up to three times!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_super_user() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", "1")
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(511, json.getInt("status"));
		assertNotNull("You are not allowed to do this operation!", json.get("reason"));
	}

	private Long findMemberIdByIndex(Cookies cookies, int index) {
		HttpResponse<JsonNode> res = Unirest.get("/membership")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");
		
		return data.getJSONObject(index).getLong("id");
	}

	/**
	 * Consists of two steps;
	 *    a) get member list to find previously added member id
	 *    b) re-sends the invitation
	 */
	@Test
	public void Everything_must_be_ok_WITH_admin_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Premium_plan_and_three_pending_users.ADMIN());
		
		//get member list to find member id
		Long memberId = findMemberIdByEmail(cookies, TestAccounts.Premium_plan_and_three_pending_users.EDITOR().getString("email"));

		//re-sends the invitation
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", memberId.toString())
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
	}

	private Long findMemberIdByEmail(Cookies cookies, String email) {
		HttpResponse<JsonNode> res = Unirest.get("/membership")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");
		
		for (int i = 0; i < data.length(); i++) {
			JSONObject jobj = data.getJSONObject(i);
			if (email.equals(jobj.getString("email"))) return jobj.getLong("id");
		}
		
		return null;
	}

}
