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
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of GroupController.delete(Long groupId)
 * 
 * @author mdpinar
 * @since 2021-07-20
 */
@RunWith(JUnit4.class)
public class DeleteTest {

	private static final String SERVICE_ENDPOINT = "/group/{id}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("id", "1")
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Page_not_found_WITH_null_id() {
		JSONObject json = callTheService(null);

		assertEquals(404, json.getInt("status"));
    assertEquals("Page not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 *	a) to gather other account's groups, admin is logged in
	 *	b) finds some specific groups
	 *  c) picks one of them
	 *  d) evil user tries to delete the group
	 */
	@Test
	public void Group_not_found_WHEN_trying_to_delete_someone_elses_group() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray groupList = TestFinder.searchGroups(cookies, "Group X");
		TestUtils.logout(cookies); //here is important!
		
		assertNotNull(groupList);
		assertEquals(1, groupList.length());

		//get the first group
		JSONObject group = groupList.getJSONObject(0);

		//evil user tries to delete the group
		JSONObject json = callTheService(group.getLong("id"));

		assertEquals(404, json.getInt("status"));
		assertEquals("Group not found!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.EDITOR());

		JSONArray groupList = TestFinder.searchGroups(cookies, "Group I");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(groupList);
		assertEquals(1, groupList.length());

		//get the first group
		JSONObject group = groupList.getJSONObject(0);

		cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+group.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_editor_and_admin() {
		Map<JSONObject, String> userGroupNameMap = Map.of(
			TestAccounts.Standard_plan_and_two_extra_users.EDITOR(), "Group K",
			TestAccounts.Standard_plan_and_two_extra_users.ADMIN(), "Group G"
		);

		for (Entry<JSONObject, String> userGroupName: userGroupNameMap.entrySet()) {
			Cookies cookies = TestUtils.login(userGroupName.getKey());

			JSONArray groupList = TestFinder.searchGroups(cookies, userGroupName.getValue());
  
  		assertNotNull(groupList);
  		assertEquals(1, groupList.length());
  
  		//get the first group
  		JSONObject group = groupList.getJSONObject(0);
  
  		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
  			.headers(Fixtures.SESSION_0_HEADERS)
  			.cookie(cookies)
  			.routeParam("id", ""+group.getLong("id"))
  			.asJson();
  		TestUtils.logout(cookies);
  
  		JSONObject json = res.getBody().getObject();
  
  		assertEquals(200, json.getInt("status"));
  		assertTrue(json.has("data"));
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

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.routeParam("id", (id != null ? id.toString() : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
