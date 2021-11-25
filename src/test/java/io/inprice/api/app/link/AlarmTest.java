package io.inprice.api.app.link;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import io.inprice.api.utils.TestWorkspaces;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of LinkController.setAlarmON/OFF(AlarmEntityDTO)
 * 
 * @author mdpinar
 * @since 2021-11-20
 */
@RunWith(Parameterized.class)
public class AlarmTest {

	private String SERVICE_ENDPOINT = "/link/alarm";

	/**
	 * This method runs this class twice
	 * 
	 */
  @Parameterized.Parameters
  public static Object[][] getHttpMethodParams() {
  	return new Object[][] { { "/on" }, { "/off" } };
  }
  
  public AlarmTest(String postfix) {
  	this.SERVICE_ENDPOINT += postfix;
  }

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.ADMIN(), null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Link_not_found_WITHOUT_link_id_set() {
		JSONObject json = callTheService(new Long[] {});

		assertEquals(404, json.getInt("status"));
		assertEquals("Link not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, new Long[] { 1L });

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER(), new Long[] { 1L }, 0);

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	/**
	 * Consists of six steps;
	 *	a) to gather other workspace's links, admin is logged in
	 *	b) searches some specific links
	 *  c) picks one of those links
	 *  d) builds body up
	 *  e) evil user logs in
	 *  f) tries to delete other workspace's links
	 */
	@Test
	public void Alarm_not_found_WHEN_trying_to_update_someone_elses_links() {
		//to gather other workspace's links, admin is logged in
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		//searches for alarms
		JSONArray alarmList = TestFinder.searchAlarms(cookies, "LINK");
		assertNotNull(alarmList);
		TestUtils.logout(cookies); //here is important!

		//evil user logs in
		cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_one_extra_user.ADMIN());

		//searches some specific links
		JSONArray linkList = TestFinder.searchLinks(cookies, "ACTIVE", 0);
		assertNotNull(linkList);

		//picks one of those links
		Long[] linkIds = { linkList.getJSONObject(0).getLong("id") };
		
		//builds the body up
		JSONObject body = new JSONObject();
		body.put("alarmId", alarmList.getJSONObject(0).getLong("id"));
		body.put("entityIdSet", linkIds);

		//tries to update other users' links
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertEquals("Alarm not found!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 *	a) searches for alarms for links
	 *	b) searches for trying links
	 *  c) builds body up with those link and alarm
	 *  d) tries to set alarm on
	 */
	@Test
	public void You_have_reached_max_alarm_number_of_your_plan() {
		if (SERVICE_ENDPOINT.endsWith("/off")) return;

		//to gather other workspace's links, admin is logged in
		Cookies cookies = TestUtils.login(TestWorkspaces.Basic_plan_but_no_extra_user_for_alarm_limits.ADMIN());

		//searches for alarms
		JSONArray alarmList = TestFinder.searchAlarms(cookies, "LINK");
		assertNotNull(alarmList);

		//searches for trying links (must be 1)
		JSONArray tryingLinksList = TestFinder.searchLinks(cookies, "TRYING");
		assertNotNull(tryingLinksList);
		assertTrue(tryingLinksList.length() == 1);

		//picks one of those links
		Long[] linkIds = { tryingLinksList.getJSONObject(0).getLong("id") };
		
		//builds the body up
		JSONObject body = new JSONObject();
		body.put("alarmId", alarmList.getJSONObject(0).getLong("id"));
		body.put("entityIdSet", linkIds);

		//tries to update other users' links
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(910, json.getInt("status"));
		assertEquals("You have reached max alarm number of your plan!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) editor or admin logs in
	 *	b) searches some specific links
	 *  c) gathers two of them
	 *  d) builds body up
	 *  e) deletes those selected links
	 */
	@Test
	public void Everything_must_be_ok_FOR_editor_and_admin() {
		//both workspace have 2 links in PROBLEM status!
		JSONObject[] users = {
			TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN(),
			TestWorkspaces.Starter_plan_and_one_extra_user.EDITOR()
		};

		for (JSONObject user: users) {
			Cookies cookies = TestUtils.login(user);

			//searches for alarms
			JSONArray alarmList = TestFinder.searchAlarms(cookies, "LINK");
  		assertNotNull(alarmList);
  
  		//searches some specific links
  		JSONArray linkList = TestFinder.searchLinks(cookies, "PROBLEM");
  
  		assertNotNull(linkList);
  		assertEquals(3, linkList.length());
  
  		//gathers two of them
  		Long[] linkIds = new Long[2];
  		
  		for (int i = 0; i < 2; i++) {
  			JSONObject link = linkList.getJSONObject(i);
  			linkIds[i] = link.getLong("id");
  		}

  		//builds the body up
  		JSONObject body = new JSONObject();
  		body.put("alarmId", alarmList.getJSONObject(0).getLong("id"));
  		body.put("entityIdSet", linkIds);
  
  		//deletes those selected links
  		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
  			.headers(Fixtures.SESSION_0_HEADERS)
  			.cookie(cookies)
  			.body(body)
  			.asJson();
  		TestUtils.logout(cookies);
  
  		JSONObject json = res.getBody().getObject();
  		assertEquals(200, json.getInt("status"));
		}
	}

	private JSONObject callTheService(Long[] linkIds) {
		return callTheService(TestWorkspaces.Standard_plan_and_no_extra_users.ADMIN(), linkIds);
	}

	private JSONObject callTheService(JSONObject user, Long[] entityIdSet) {
		return callTheService(user, entityIdSet, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long[] entityIdSet, int session) {
		Cookies cookies = TestUtils.login(user);
		
		JSONObject body = null;
		if (entityIdSet != null) {
			body = new JSONObject();
			if (entityIdSet != null && entityIdSet.length > 0) body.put("entityIdSet", entityIdSet);
		}

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
