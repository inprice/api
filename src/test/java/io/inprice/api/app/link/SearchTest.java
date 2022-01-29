package io.inprice.api.app.link;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
 * Tests the functionality of LinkController.search(SearchDTO)
 * 
 * @author mdpinar
 * @since 2021-07-19
 */
@RunWith(JUnit4.class)
public class SearchTest {

	private static final String SERVICE_ENDPOINT = "/links/search";

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
	public void You_must_bind_an_workspace_WITH_superuser_WITHOUT_binding_workspace() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, createBody(null, "ALL"));

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind to a workspace!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) searches a specific workspace
	 * 	c) gets link list (must not be empty)
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_workspace() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);
		JSONArray workspaces = TestFinder.searchWorkspaces(cookies, TestWorkspaces.Premium_plan_and_two_extra_users.getName());

		assertEquals(1, workspaces.length());

		JSONObject workspace = workspaces.getJSONObject(0);
		
		HttpResponse<JsonNode> res = Unirest.put("/sys/workspace/bind/{workspaceId}")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("workspaceId", ""+workspace.getLong("id"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));
		
		res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody(null, "ALL"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONArray rows = json.getJSONArray("data");
		assertEquals(12, rows.length());
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		JSONObject json = callTheService(TestWorkspaces.Professional_plan_and_one_extra_user.ADMIN(), createBody(new String[] { "WAITING" }, "ALARMED"));
		assertEquals(200, json.getInt("status"));

		JSONArray rows = json.getJSONArray("data");
		assertEquals(1, rows.length());
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		JSONObject json = callTheService(TestWorkspaces.Professional_plan_and_one_extra_user.EDITOR(), createBody(new String[] { "PROBLEM" }, "NOT_ALARMED"));
		assertEquals(200, json.getInt("status"));

		JSONArray rows = json.getJSONArray("data");
		assertEquals(3, rows.length());
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER(), createBody(new String[] { "ACTIVE" }, "NOT_ALARMED"), 0); //attention!
		assertEquals(200, json.getInt("status"));

		JSONArray rows = json.getJSONArray("data");
		assertEquals(6, rows.length());
	}

	private JSONObject createBody(String[] statuses, String alarmStatus) {
		JSONObject body = new JSONObject();
		if (statuses != null) body.put("statuses", statuses);
		if (alarmStatus != null) body.put("alarmStatus", alarmStatus);

		return body;
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		return callTheService(user, body, 0);
	}

	private JSONObject callTheService(JSONObject user, JSONObject body, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
