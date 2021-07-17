package io.inprice.api.app.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of SubscriptionService.startFreeUse()
 * 
 * @author mdpinar
 * @since 2021-07-17
 */
@RunWith(JUnit4.class)
public class StartFreeUseTest {

	private static final String SERVICE_ENDPOINT = "/subscription/free-use";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}
	
	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer_login() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());
		
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser_login() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);
		
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.cookie(cookies)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void You_have_no_free_use_right_FOR_not_suitable_account() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());
		
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.cookie(cookies)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(711, json.getInt("status"));
    assertEquals("You have no free use right!", json.getString("reason"));
	}

	@Test
	public void You_have_already_used_your_free_use() {
		Cookies cookies = TestUtils.login(TestAccounts.Second_without_a_plan_and_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.cookie(cookies)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(805, json.getInt("status"));
    assertEquals("You have already used your free use!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin_login() {
		Cookies cookies = TestUtils.login(TestAccounts.Without_a_plan_and_extra_user.ADMIN());
		
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.cookie(cookies)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
    assertTrue(json.has("data"));
	}

}
