package io.inprice.api.app.group;

import static org.junit.Assert.assertEquals;
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
 * Tests the functionality of GroupController.search(SearchDTO)
 * 
 * @author mdpinar
 * @since 2021-07-20
 */
@RunWith(JUnit4.class)
public class SearchTest {

	private static final String SERVICE_ENDPOINT = "/groups/search";

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
	public void You_must_bind_an_account_WITH_superuser_WITHOUT_binding_account() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, "Group A");

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an account!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) searches a specific account
	 * 	c) gets link list (must not be empty)
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_account() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		JSONArray accounts = TestFinder.searchAccounts(cookies, TestAccounts.Standard_plan_and_two_extra_users.getName());

		assertEquals(1, accounts.length());

		JSONObject account = accounts.getJSONObject(0);
		
		HttpResponse<JsonNode> res = Unirest.put("/sys/account/bind/{accountId}")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("accountId", ""+account.getLong("xid"))
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

		JSONArray data = json.getJSONArray("data");
		assertEquals(3, data.length());
	}

	@Test
	public void Everything_must_be_ok_FOR_any_kind_of_users() {
		Map<TestRoles, JSONObject> roleUserMap = new HashMap<>(3);
		roleUserMap.put(TestRoles.ADMIN, TestAccounts.Standard_plan_and_two_extra_users.ADMIN());
		roleUserMap.put(TestRoles.EDITOR, TestAccounts.Standard_plan_and_two_extra_users.EDITOR());
		roleUserMap.put(TestRoles.VIEWER, TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue(), "Group K", (TestRoles.VIEWER.equals(roleUser.getKey()) ? 1 : 0));

			assertEquals(roleUser.getKey().name(), 200, json.getInt("status"));
  		assertTrue(json.has("data"));

  		assertEquals(1, json.getJSONArray("data").length());
		}
	}

	public JSONObject createBody(String[] statuses, String alarmStatus) {
		JSONObject body = new JSONObject();
		if (statuses != null) body.put("statuses", statuses);
		if (alarmStatus != null) body.put("alarmStatus", alarmStatus);

		return body;
	}
	
	private JSONObject callTheService(JSONObject user, String byName) {
		return callTheService(user, byName, 0);
	}

	private JSONObject callTheService(JSONObject user, String byName, int session) {
		Cookies cookies = TestUtils.login(user);
		
		JSONObject body = new JSONObject();
		if (byName != null) body.put("term", byName);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
