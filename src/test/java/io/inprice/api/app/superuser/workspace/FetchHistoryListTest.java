package io.inprice.api.app.superuser.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of superuser's Workspace -> Controller.fetchHistoryList(Long workspaceId)
 * 
 * @author mdpinar
 * @since 2021-07-23
 */
@RunWith(JUnit4.class)
public class FetchHistoryListTest {

	private static final String SERVICE_ENDPOINT = "/sys/workspace/details/history/{workspaceId}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("workspaceId", "1")
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	/**
	 * Fulfills the same test for three types of users; viewer, editor and admin.
	 */
	@Test
	public void Forbidden_WITH_normal_users() {
		for (JSONObject user: Fixtures.NORMAL_USER_LIST) {
			JSONObject json = callTheService(user, 1L);

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}
	
	@Test
	public void Workspace_not_found_WITH_non_existing_id() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 999L);

		assertEquals(200, json.getInt("status"));
		assertEquals(0, json.getJSONArray("data").length());
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		JSONArray workspaceList = TestFinder.searchWorkspaces(cookies, "With Standard Plan and Two Extra Users");
		assertNotNull(workspaceList);
		assertEquals(1, workspaceList.length());
		
		JSONObject workspace = workspaceList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.cookie(cookies)
			.routeParam("workspaceId", ""+workspace.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONArray data = json.getJSONArray("data");

		assertEquals(2, data.length());
	}
	
	private JSONObject callTheService(JSONObject user, Long workspaceId) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.cookie(cookies)
			.routeParam("workspaceId", (workspaceId != null ? ""+workspaceId : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
