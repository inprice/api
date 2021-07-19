package io.inprice.api.app.alarm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of AlarmController.search(SearchDTO)
 * 
 * @author mdpinar
 * @since 2021-07-15
 */
@RunWith(JUnit4.class)
public class SearchTest {

	private static final String SERVICE_ENDPOINT = "/alarms/search";

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
		JSONObject json = callTheService(Fixtures.SUPER_USER, createBody(null));

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an account!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) binds to first account
	 * 	c) gets alarm list (possibly empty)
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_account() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.put("/sys/account/bind/1")
			.cookie(cookies)
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));
		
		res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(createBody("LINK"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		JSONObject json = callTheService(TestAccounts.Starter_plan_and_one_extra_user.ADMIN(), createBody("LINK"));

		JSONObject data = json.getJSONObject("data");
		JSONArray rows = data.getJSONArray("rows");
		
		assertEquals(200, json.getInt("status"));
		assertEquals(1, rows.length());
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		JSONObject json = callTheService(TestAccounts.Starter_plan_and_one_extra_user.EDITOR(), createBody("GROUP"));

		JSONObject data = json.getJSONObject("data");
		JSONArray rows = data.getJSONArray("rows");
		
		assertEquals(200, json.getInt("status"));
		assertEquals(1, rows.length());
	}

	@Test
	public void Everything_must_be_ok_WITH_mixin() {
		JSONObject json = callTheService(TestAccounts.Starter_plan_and_one_extra_user.EDITOR(), createBody(null));

		JSONObject data = json.getJSONObject("data");
		JSONArray rows = data.getJSONArray("rows");
		
		assertEquals(200, json.getInt("status"));
		assertEquals(2, rows.length());
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), createBody(null), 1); //attention!

		JSONObject data = json.getJSONObject("data");
		JSONArray rows = data.getJSONArray("rows");
		
		assertEquals(200, json.getInt("status"));
		assertEquals(0, rows.length());
	}

	public JSONObject createBody(String topic) {
		JSONObject body = new JSONObject();
		if (topic != null) body.put("topic", topic);

		return body;
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		return callTheService(user, body, 0);
	}

	private JSONObject callTheService(JSONObject user, JSONObject body, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
