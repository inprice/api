package io.inprice.api.app.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 * Tests the functionality of AlarmService.delete(Long alarmId)
 * 
 * @author mdpinar
 * @since 2021-07-15
 */
@RunWith(JUnit4.class)
public class DeleteTest {

	private static final String SERVICE_ENDPOINT = "/alarm/{id}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITH_no_session() {
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
	public void Alarm_not_found_WITH_wrong_id() {
		JSONObject json = callTheService(0L);

		assertEquals(404, json.getInt("status"));
		assertEquals("Alarm not found!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), 1L, 1); //attention!

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_super_user() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_link_WITH_admin() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray alarmedLinkList = TestFinder.searchAlarms(cookies, "LINK");

		assertNotNull(alarmedLinkList);
		assertEquals(1, alarmedLinkList.length());

		//get the first alarm for a link
		JSONObject alarmedLink = alarmedLinkList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+alarmedLink.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_group_WITH_editor() {
		Cookies cookies = TestUtils.login(TestAccounts.Starter_plan_and_one_extra_user.EDITOR());

		JSONArray alarmedGroupList = TestFinder.searchAlarms(cookies, "GROUP");

		assertNotNull(alarmedGroupList);
		assertEquals(1, alarmedGroupList.length());

		//get the first alarm for a group
		JSONObject alarmedGroup = alarmedGroupList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+alarmedGroup.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	public JSONObject callTheService(Long id) {
		return callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), id);
	}

	public JSONObject callTheService(JSONObject user, Long id) {
		return callTheService(user, id, 0);
	}
	
	public JSONObject callTheService(JSONObject user, Long id, int session) {
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
