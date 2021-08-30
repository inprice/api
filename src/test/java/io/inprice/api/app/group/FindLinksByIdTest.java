package io.inprice.api.app.group;

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
 * Tests the functionality of GroupController.findLinksById(Long groupId)
 * 
 * @author mdpinar
 * @since 2021-07-20
 */
@RunWith(JUnit4.class)
public class FindLinksByIdTest {

	private static final String SERVICE_ENDPOINT = "/group/links/{id}";

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

	/**
	 * Consists of four steps;
	 *	a) to gather other account's groups, admin is logged in
	 *	b) finds some specific groups
	 *  c) picks one of them
	 *  d) evil user tries to find the group
	 */
	@Test
	public void Group_not_found_WHEN_trying_to_find_someone_elses_group() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray groupList = TestFinder.searchGroups(cookies, "Group X");
		TestUtils.logout(cookies); //here is important!
		
		assertNotNull(groupList);
		JSONObject group = groupList.getJSONObject(0);

		//evil user tries to find the group
		JSONObject json = callTheService(group.getLong("id"));

		assertEquals(404, json.getInt("status"));
		assertEquals("Group not found!", json.getString("reason"));
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
			.routeParam("accountId", ""+account.getLong("id"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		JSONArray groupList = TestFinder.searchGroups(cookies, "Group D");
		JSONObject group = groupList.getJSONObject(0);
		
		res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+group.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		JSONObject data = json.getJSONObject("data");
		
		assertTrue(data.has("group"));
		assertTrue(data.has("links"));
		
		JSONArray links = data.getJSONArray("links");
		assertEquals(1, links.length());
	}

	@Test
	public void Everything_must_be_ok_FOR_any_kind_of_users() {
		Map<TestRoles, JSONObject> roleUserMap = Map.of(
			TestRoles.ADMIN, TestAccounts.Standard_plan_and_two_extra_users.ADMIN(),
			TestRoles.EDITOR, TestAccounts.Standard_plan_and_two_extra_users.EDITOR(),
			TestRoles.VIEWER, TestAccounts.Standard_plan_and_two_extra_users.VIEWER()
		);

		Cookies cookies = TestUtils.login(roleUserMap.get(TestRoles.ADMIN));

		JSONArray groupList = TestFinder.searchGroups(cookies, "Group G");
		TestUtils.logout(cookies);

		assertNotNull(groupList);
		assertEquals(1, groupList.length());

		//get the first group
		JSONObject group = groupList.getJSONObject(0);

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue(), group.getLong("id"), (TestRoles.VIEWER.equals(roleUser.getKey()) ? 1 : 0));

			assertEquals(roleUser.getKey().name(), 200, json.getInt("status"));
			assertTrue(json.has("data"));

			JSONObject data = json.getJSONObject("data");
			
			assertTrue(data.has("group"));
			assertTrue(data.has("links"));
  		
  		JSONArray links = data.getJSONArray("links");
  		assertEquals(4, links.length());
		}
	}

	private JSONObject callTheService(Long id) {
		return callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), id);
	}

	private JSONObject callTheService(JSONObject user, Long id) {
		return callTheService(user, id, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long id, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.routeParam("id", (id != null ? id.toString() : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
