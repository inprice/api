package io.inprice.api.app.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of SubscriptionController.cancel() 
 * 
 * @author mdpinar
 * @since 2021-07-18
 */
@RunWith(JUnit4.class)
public class CancelTest {

	private static final String SERVICE_ENDPOINT = "/subscription/cancel";

	@BeforeClass
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
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheService(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER(), 1); //attention pls!

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_editor() {
		JSONObject json = callTheService(TestWorkspaces.Premium_plan_and_two_extra_users.EDITOR());

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
	}

	@Test
	public void You_dont_have_an_active_plan_to_cancel_FOR_a_cancelled_workspace() {
		JSONObject json = callTheService(TestWorkspaces.Cancelled_Standard_plan.ADMIN());

		assertEquals(710, json.getInt("status"));
		assertEquals("You don't have an active plan to cancel!", json.get("reason"));
	}

	@Test
	public void You_dont_have_an_active_plan_to_cancel_FOR_an_workspace_WITH_no_plan() {
		JSONObject json = callTheService(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN());

		assertEquals(710, json.getInt("status"));
		assertEquals("You don't have an active plan to cancel!", json.get("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_an_workspace_WITH_FREE_USE() {
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_no_extra_user.ADMIN());

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_FOR_an_workspace_WITH_VOUCHERED() {
		JSONObject json = callTheService(TestWorkspaces.Second_standard_plan_and_no_extra_user.ADMIN());

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	private JSONObject callTheService(JSONObject user) {
		return callTheService(user, 0);
	}
	
	private JSONObject callTheService(JSONObject user, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);
		
		return res.getBody().getObject();
	}

}
