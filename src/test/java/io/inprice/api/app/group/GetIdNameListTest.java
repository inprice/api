package io.inprice.api.app.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
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
 * Tests the functionality of GroupController.getIdNameList(Long excludedGroupId)
 * 
 * @author mdpinar
 * @since 2021-07-20
 */
@RunWith(JUnit4.class)
public class GetIdNameListTest {

	private static final String SERVICE_ENDPOINT = "/group/pairs/{excludedGroupId}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Request_body_is_invalid_WITH_null_id() {
		JSONObject json = callTheService(null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void You_must_bind_an_account_WITH_superuser_WITHOUT_binding_account() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an account!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) binds to a specific account
	 * 	c) gets group list (must not be empty)
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_account() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);
		
		JSONArray accountList = TestFinder.searchAccounts(cookies, "With Standard Plan (Couponed) but No Extra User");
		JSONObject account = accountList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put("/sys/account/bind/{accountId}")
			.cookie(cookies)
			.routeParam("accountId", ""+account.getLong("xid"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		JSONArray groupList = TestFinder.searchGroups(cookies, "Group D");
		JSONObject group = groupList.getJSONObject(0);
		
		res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("excludedGroupId", ""+group.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_FOR_any_kind_of_users() {
		Map<TestRoles, JSONObject> roleUserMap = new HashMap<>(3);
		roleUserMap.put(TestRoles.ADMIN, TestAccounts.Standard_plan_and_two_extra_users.ADMIN());
		roleUserMap.put(TestRoles.EDITOR, TestAccounts.Standard_plan_and_two_extra_users.EDITOR());
		roleUserMap.put(TestRoles.VIEWER, TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue(), 0L, (TestRoles.VIEWER.equals(roleUser.getKey()) ? 1 : 0));

			assertEquals(200, json.getInt("status"));
			assertTrue(json.has("data"));

			JSONArray data = json.getJSONArray("data");
			assertEquals(2, data.length());
		}
	}

	@Test
	public void Everything_must_be_ok_WITH_excluded_id() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.EDITOR());

		JSONArray groupList = TestFinder.searchGroups(cookies, "Group X");
		TestUtils.logout(cookies); //here is important!
		
		assertNotNull(groupList);
		JSONObject group = groupList.getJSONObject(0);

		//evil user tries to find the group
		JSONObject json = callTheService(TestAccounts.Starter_plan_and_one_extra_user.EDITOR(), group.getLong("id"));

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONArray data = json.getJSONArray("data");
		assertEquals(1, data.length());
		assertTrue(data.getJSONObject(0).getString("right").startsWith("Group Y"));
	}

	private JSONObject callTheService(Long excludedGroupId) {
		return callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), excludedGroupId);
	}

	private JSONObject callTheService(JSONObject user, Long excludedGroupId) {
		return callTheService(user, excludedGroupId, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long excludedGroupId, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.routeParam("excludedGroupId", (excludedGroupId != null ? excludedGroupId.toString() : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
