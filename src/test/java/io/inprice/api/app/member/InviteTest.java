package io.inprice.api.app.member;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.app.utils.Fixtures;
import io.inprice.api.app.utils.TestAccount;
import io.inprice.api.app.utils.TestRole;
import io.inprice.api.app.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * No need to check email limitations again here since it is already done in Login test class
 * 
 * @author mdpinar
 * @since 2021-07-03
 */
@RunWith(JUnit4.class)
public class InviteTest {

	private static final String SERVICE_ENDPOINT = "/member";

	private static final JSONObject ADMIN_X = Fixtures.NORMAL_USER(TestRole.ADMIN, TestAccount.X);
	private static final JSONObject EDITOR_X = Fixtures.NORMAL_USER(TestRole.EDITOR, TestAccount.X);

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Request_body_is_invalid_WITH_no_body() {
		Cookies cookies = TestUtils.login(TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Email_address_cannot_be_empty_WITH_empty_email() {
		Cookies cookies = TestUtils.login(TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(null, TestRole.EDITOR))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Email address cannot be empty!", json.getString("reason"));
	}

	@Test
	public void You_cannot_invite_yourself_WITH_same_email() {
		Cookies cookies = TestUtils.login(TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(ADMIN_X.getString("email"), null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("You cannot invite yourself!", json.getString("reason"));
	}

	@Test
	public void Role_must_be_either_EDITOR_or_VIEWER_WITH_empty_role() {
		Cookies cookies = TestUtils.login(TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(EDITOR_X.getString("email"), null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Role must be either EDITOR or VIEWER!", json.getString("reason"));
	}

	@Test
	public void Role_must_be_either_EDITOR_or_VIEWER_WITH_ADMIN_role() {
		Cookies cookies = TestUtils.login(TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(EDITOR_X.getString("email"), TestRole.ADMIN))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Role must be either EDITOR or VIEWER!", json.getString("reason"));
	}

	@Test
	public void Role_must_be_either_EDITOR_or_VIEWER_WITH_SUPER_role() {
		Cookies cookies = TestUtils.login(TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(EDITOR_X.getString("email"), TestRole.SUPER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Role must be either EDITOR or VIEWER!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer_user() {
		Cookies cookies = TestUtils.login(TestRole.VIEWER);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(Fixtures.NORMAL_USER(TestRole.EDITOR, TestAccount.Y).getString("email"), null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_editor_user() {
		Cookies cookies = TestUtils.login(TestRole.EDITOR);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(Fixtures.NORMAL_USER(TestRole.EDITOR, TestAccount.Y).getString("email"), null))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void You_need_to_subscribe_to_a_plan_WITH_no_plan() {
		Cookies cookies = TestUtils.login(TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(EDITOR_X.getString("email"), TestRole.EDITOR))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(503, json.getInt("status"));
    assertEquals("You need to subscribe to a plan!", json.getString("reason"));
	}
	
	@Test
	public void Your_user_count_is_reached_your_plans_limit_WITH_a_plan_less_user_limit() {
		Cookies cookies = TestUtils.login(TestAccount.Z, TestRole.ADMIN);
		
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(EDITOR_X.getString("email"), TestRole.EDITOR))
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(506, json.getInt("status"));
		assertEquals("Your user count is reached your plans limit!", json.getString("reason"));
	}

	@Test
	public void This_user_has_already_been_added_to_this_account_WITH_a_plan_less_user_limit() {
		Cookies cookies = TestUtils.login(TestAccount.S, TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(Fixtures.NORMAL_USER(TestRole.EDITOR, TestAccount.S).getString("email"), TestRole.VIEWER))
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
		Cookies cookies = TestUtils.login(TestAccount.S, TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(Fixtures.BANNED_USER.getString("email"), TestRole.VIEWER))
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
		Cookies cookies = TestUtils.login(TestRole.SUPER);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(Fixtures.NORMAL_USER(TestRole.EDITOR, TestAccount.Y).getString("email"), null))
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
		Cookies cookies = TestUtils.login(TestAccount.S, TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(Fixtures.SUPER_USER.getString("email"), TestRole.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(511, json.getInt("status"));
    assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	/**
	 * Admin of S invites the Viewer of Z as Viewer!
	 */
	@Test
	public void Everything_must_be_OK_WITH_correct_parameters() {
		Cookies cookies = TestUtils.login(TestAccount.S, TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.body(createBody(Fixtures.NORMAL_USER(TestRole.VIEWER, TestAccount.Z).getString("email"), TestRole.VIEWER))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
    assertEquals("OK", json.getString("reason"));
	}

	private JSONObject createBody(String email, TestRole role) {
		JSONObject body = new JSONObject();
		if (email != null) body.put("email", email);
		if (role != null) body.put("role", role.name());
		return body;
	}

}
