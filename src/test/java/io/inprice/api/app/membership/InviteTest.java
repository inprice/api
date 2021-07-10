package io.inprice.api.app.membership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of MembershipService.invite(InvitationSendDTO)
 * 
 * Out of scope:
 * 	- No need to check email limitations again here since it is already done in Login test class
 * 
 * @author mdpinar
 * @since 2021-07-03
 */
@RunWith(JUnit4.class)
public class InviteTest {

	private static final String SERVICE_ENDPOINT = "/membership";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Request_body_is_invalid_WITH_no_body() {
		Cookies cookies = TestUtils.login(TestAccounts.Basic_plan_but_no_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Email_address_cannot_be_empty_WITH_empty_email() {
		Cookies cookies = TestUtils.login(TestAccounts.Basic_plan_but_no_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(null, TestRoles.EDITOR))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address cannot be empty!", json.getString("reason"));
	}

	@Test
	public void You_cannot_invite_yourself_WITH_same_email() {
		Cookies cookies = TestUtils.login(TestAccounts.Basic_plan_but_no_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(TestAccounts.Basic_plan_but_no_extra_user.getEmail(TestRoles.ADMIN), null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("You cannot invite yourself!", json.getString("reason"));
	}

	@Test
	public void Role_must_be_either_EDITOR_or_VIEWER_WITH_empty_role() {
		Cookies cookies = TestUtils.login(TestAccounts.Basic_plan_but_no_extra_user.ADMIN());
		
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(TestAccounts.Starter_plan_and_one_extra_user.getEmail(TestRoles.EDITOR), null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Role must be either EDITOR or VIEWER!", json.getString("reason"));
	}

	@Test
	public void Role_must_be_either_EDITOR_or_VIEWER_WITH_ADMIN_role() {
		Cookies cookies = TestUtils.login(TestAccounts.Basic_plan_but_no_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(TestAccounts.Starter_plan_and_one_extra_user.getEmail(TestRoles.EDITOR), TestRoles.ADMIN))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Role must be either EDITOR or VIEWER!", json.getString("reason"));
	}

	@Test
	public void Role_must_be_either_EDITOR_or_VIEWER_WITH_SUPER_role() {
		Cookies cookies = TestUtils.login(TestAccounts.Pro_plan_and_extra_one_pending_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(TestAccounts.Starter_plan_and_one_extra_user.getEmail(TestRoles.EDITOR), TestRoles.SUPER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Role must be either EDITOR or VIEWER!", json.getString("reason"));
	}

	@Test
	public void Forbidden_FOR_viewer_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS) //in his 0th session, he is an ADMIN!
			.cookie(cookies)
			.body(createBody(TestAccounts.Basic_plan_but_no_extra_user.getEmail(TestRoles.ADMIN), TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Forbidden_FOR_editor_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.EDITOR());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(TestAccounts.Starter_plan_and_one_extra_user.getEmail(TestRoles.EDITOR), TestRoles.EDITOR))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void You_need_to_subscribe_to_a_plan_WITH_no_plan() {
		Cookies cookies = TestUtils.login(TestAccounts.Without_a_plan_and_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS) 
			.cookie(cookies)
			.body(createBody(TestAccounts.Starter_plan_and_one_extra_user.getEmail(TestRoles.EDITOR), TestRoles.EDITOR))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(503, json.getInt("status"));
    assertEquals("You need to subscribe to a plan!", json.getString("reason"));
	}
	
	@Test
	public void Your_user_count_is_reached_your_plans_limit_WITH_a_plan_less_user_limit() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());
		
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(TestAccounts.Starter_plan_and_one_extra_user.getEmail(TestRoles.EDITOR), TestRoles.EDITOR))
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(506, json.getInt("status"));
		assertEquals("Your user count is reached your plans limit!", json.getString("reason"));
	}

	@Test
	public void This_user_has_already_been_added_to_this_account_FOR_already_added_email() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_one_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(TestAccounts.Standard_plan_and_one_extra_user.getEmail(TestRoles.EDITOR), TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(400, json.getInt("status"));
    assertEquals("This user has already been added to this account!", json.getString("reason"));
	}

	/**
	 * Admin of S invites a banned user!
	 */
	@Test
	public void Banned_user_WITH_banned_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(Fixtures.BANNED_USER.getString("email"), TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
    assertEquals("Banned user!", json.getString("reason"));
	}

	/**
	 * A super user invites another user
	 */
	@Test
	public void You_are_not_allowed_to_do_this_operation_AS_super_user() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(TestAccounts.Starter_plan_and_one_extra_user.getEmail(TestRoles.EDITOR), null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(511, json.getInt("status"));
		assertNotNull("You are not allowed to do this operation!", json.get("reason"));
	}

	/**
	 * Admin of S invites a super user!
	 */
	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_super_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(Fixtures.SUPER_USER.getString("email"), TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(511, json.getInt("status"));
    assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	/**
	 * Admin of S invites a non-existing user!
	 */
	@Test
	public void Everything_must_be_OK_WITH_a_non_existing_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Pro_plan_and_extra_one_pending_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(Fixtures.NON_EXISTING_EMAIL_1, TestRoles.EDITOR))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		assertNotNull(data.get("url"));
		assertNotNull(data.getString("token"));
		assertTrue(Fixtures.NON_EXISTING_EMAIL_1.startsWith(data.getString("user")));
	}

	/**
	 * Admin of S invites the Viewer of Z as Viewer!
	 */
	@Test
	public void Everything_must_be_OK_WITH_an_existing_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Pro_plan_and_extra_one_pending_user.ADMIN());
		String email = TestAccounts.Starter_plan_and_one_extra_user.getEmail(TestRoles.EDITOR);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(createBody(email, TestRoles.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		assertFalse(data.has("url"));
		assertTrue(email.startsWith(data.getString("user")));
	}

	private JSONObject createBody(String email, TestRoles role) {
		JSONObject body = new JSONObject();
		if (email != null) body.put("email", email);
		if (role != null) body.put("role", role.name());
		return body;
	}

}
