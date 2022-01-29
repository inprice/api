package io.inprice.api.app.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
 * Tests the functionality of AlarmController.delete(Long alarmId)
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

	/**
	 * Consists of four steps;
	 *	a) to gather other workspace's alarms, admin is logged in
	 *	b) searches some specific alarms
	 *  c) picks one of those alarms
	 *  d) evil user tries to delete other workspace's alarm
	 */
	@Test
	public void Alarm_not_found_WHEN_trying_to_delete_someone_elses_alarm() {
		//to gather other workspace's links, admin is logged in
		Cookies cookies = TestUtils.login(TestWorkspaces.Professional_plan_and_one_extra_user.ADMIN());

		//searches some specific links
		JSONArray alarmList = TestFinder.searchAlarms(cookies, "PRODUCT");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(alarmList);

		//picks one of those alarms
		JSONObject alarm = alarmList.getJSONObject(0);

		//evil user tries to delete the alarm
		JSONObject json = callTheService(alarm.getLong("id"));

		assertEquals(404, json.getInt("status"));
		assertEquals("Alarm not found!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER(), 8L);

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_a_link_WITH_admin() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Professional_plan_and_one_extra_user.ADMIN());

		JSONArray alarmedLinkList = TestFinder.searchAlarms(cookies, "LINK");

		assertNotNull(alarmedLinkList);
		assertTrue(alarmedLinkList.length() > 1);

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
	public void Everything_must_be_ok_FOR_a_product_WITH_editor() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Professional_plan_and_one_extra_user.EDITOR());

		JSONArray alarmedProductList = TestFinder.searchAlarms(cookies, "PRODUCT");

		assertNotNull(alarmedProductList);
		assertTrue(alarmedProductList.length() > 1);

		//get the first alarm for a product
		JSONObject alarmedProduct = alarmedProductList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+alarmedProduct.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private JSONObject callTheService(Long id) {
		return callTheService(TestWorkspaces.Standard_plan_and_no_extra_user.ADMIN(), id);
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
