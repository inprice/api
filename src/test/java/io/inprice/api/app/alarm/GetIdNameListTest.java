package io.inprice.api.app.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
 * Tests the functionality of AlarmController.getIdNameList(String topic)
 * 
 * @author mdpinar
 * @since 2021-11-20
 */
@RunWith(JUnit4.class)
public class GetIdNameListTest {

	private static final String SERVICE_ENDPOINT = "/alarm/pairs/{topic}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Invalid_alarm_topic_WITH_wrong_topic() {
		JSONObject json = callTheService("wrong");

		assertEquals(109, json.getInt("status"));
    assertEquals("Invalid alarm topic!", json.getString("reason"));
	}

	@Test
	public void You_must_bind_an_workspace_WITH_superuser_WITHOUT_binding_workspace() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, "LINK");

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an workspace!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) binds to a specific workspace
	 * 	c) gets alarm list (must not be empty)
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_workspace() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);
		
		JSONArray workspaceList = TestFinder.searchWorkspaces(cookies, "With Standard Plan (Vouchered) but No Extra User");
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
			.routeParam("topic", ""+alarm.getString("topic"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_FOR_any_kind_of_users() {
		Map<TestRoles, JSONObject> roleUserMap = Map.of(
			TestRoles.ADMIN, TestWorkspaces.Standard_plan_and_two_extra_users.ADMIN(),
			TestRoles.EDITOR, TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(),
			TestRoles.VIEWER, TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER()
		);

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue(), "PRODUCT", 0);

			assertEquals(200, json.getInt("status"));
			assertTrue(json.has("data"));

			JSONArray data = json.getJSONArray("data");
			assertEquals(roleUser.getKey().name(), 1, data.length());
		}
	}

	@Test
	public void Everything_must_be_ok() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.EDITOR());

		JSONArray alarmList = TestFinder.searchAlarms(cookies, "LINK");
		TestUtils.logout(cookies); //here is important!
		
		assertNotNull(alarmList);
		JSONObject alarm = alarmList.getJSONObject(0);

		//evil user tries to find the alarm
		JSONObject json = callTheService(TestWorkspaces.Starter_plan_and_one_extra_user.EDITOR(), alarm.getString("topic"));

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONArray data = json.getJSONArray("data");
		assertEquals(2, data.length());
	}

	private JSONObject callTheService(String topic) {
		return callTheService(TestWorkspaces.Basic_plan_but_no_extra_user.ADMIN(), topic);
	}

	private JSONObject callTheService(JSONObject user, String topic) {
		return callTheService(user, topic, 0);
	}
	
	private JSONObject callTheService(JSONObject user, String topic, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.routeParam("topic", (topic != null ? topic : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
