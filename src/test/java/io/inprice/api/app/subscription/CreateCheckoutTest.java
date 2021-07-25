package io.inprice.api.app.subscription;

import static org.junit.Assert.assertEquals;

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
 * Tests the functionality of SubscriptionController.createCheckout(Integer planId)
 * 
 * @author mdpinar
 * @since 2021-07-18
 */
@RunWith(JUnit4.class)
public class CreateCheckoutTest {

	private static final String SERVICE_ENDPOINT = "/subscription/create-checkout/{planId}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("planId", "1")
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER);
		
		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_editor() {
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.EDITOR());
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), 1); //attention pls!
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void Page_not_found_WITH_null_id() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("planId", "")
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
    assertEquals("Page not found!", json.getString("reason"));
	}

	@Test
	public void Method_not_allowed_WITH_admin() { //later this will become Everything must be ok!
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		assertEquals(405, json.getInt("status"));
    assertEquals("Method not allowed!", json.getString("reason"));
	}

	private JSONObject callTheService(JSONObject user) {
		return callTheService(user, 0);
	}
	
	private JSONObject callTheService(JSONObject user, int session) {
		Cookies cookies = TestUtils.login(user);
		
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.routeParam("planId", "1")
			.asJson();
		TestUtils.logout(cookies);
		
		return res.getBody().getObject();
	}

}
