package io.inprice.api.app.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
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
 * Tests the functionality of AlarmController.getDetails(Long alarmId) 
 * 
 * @author mdpinar
 * @since 2021-11-20
 */
@RunWith(JUnit4.class)
public class GetDetailsTest {

	private static final String SERVICE_ENDPOINT = "/alarm/details/{id}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Forbidden_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.routeParam("id", "1")
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
	}

	@Test
	public void You_must_bind_an_workspace_WITH_superuser_WITHOUT_binding_workspace() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", "1")
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an workspace!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 * 	a) super user logs in
	 * 	b) binds to a specific workspace
	 * 	c) finds link list
	 * 	d) fetches the details of first link
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_workspace() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);
		
		JSONArray workspaceList = TestFinder.searchWorkspaces(cookies, "With Basic Plan (Free Use) but No Extra User");
		JSONObject workspace = workspaceList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put("/sys/workspace/bind/{workspaceId}")
			.cookie(cookies)
			.routeParam("workspaceId", ""+workspace.getLong("id"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));
		
		JSONArray alarmList = TestFinder.searchAlarms(cookies, "LINK");
		JSONObject alarm = alarmList.getJSONObject(0);

		res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+alarm.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	/**
	 * Fulfills the same test for three types of users; viewer, editor and admin.
	 * 
	 */
	@Test
	public void Everything_must_be_ok_WITH_anyone() {
		Map<TestRoles, JSONObject> roleUserMap = Map.of(
			TestRoles.VIEWER, TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER(),
			TestRoles.EDITOR, TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(),
			TestRoles.ADMIN, TestWorkspaces.Standard_plan_and_two_extra_users.ADMIN()
		);

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue());
  		assertEquals(200, json.getInt("status"));
  
  		JSONObject data = json.getJSONObject("data");
  		assertTrue(data.has("alarm"));
  		assertTrue(data.has("products"));
  		assertTrue(data.has("links"));
		}
	}
	
	/**
	 * Consists of three steps;
	 * 	a) user logs in
	 * 	b) finds link list
	 * 	c) fetches the details of first link
	 */
	private JSONObject callTheService(JSONObject user) {
		Cookies cookies = TestUtils.login(user);

		JSONArray alarmList = TestFinder.searchAlarms(cookies, "LINK");
		JSONObject alarm = alarmList.getJSONObject(0);
		
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+alarm.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);
		
		return res.getBody().getObject();
	}

}
