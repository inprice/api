package io.inprice.api.app.subscription;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
 * Tests the functionality of SubscriptionController.getInfo() 
 * 
 * @author mdpinar
 * @since 2021-07-18
 */
@RunWith(JUnit4.class)
public class GetInfoTest {

	private static final String SERVICE_ENDPOINT = "/subscription/get-info";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Forbidden_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT).asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
	}

	@Test
	public void You_must_bind_an_account_WITH_superuser_WITHOUT_binding_account() {
		JSONObject json = callTheService(Fixtures.SUPER_USER);

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an account!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) binds to a specific account
	 * 	c) gets info
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_account() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);
		
		JSONArray accountList = TestFinder.searchAccounts(cookies, "Without A Plan and Extra User");
		JSONObject account = accountList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put("/sys/account/bind/{accountId}")
			.cookie(cookies)
			.routeParam("accountId", ""+account.getLong("id"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.EDITOR());
		assertEquals(200, json.getInt("status"));
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), 1); //attention pls!
		assertEquals(200, json.getInt("status"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());
		assertEquals(200, json.getInt("status"));

		JSONObject data = json.getJSONObject("data");
		assertTrue(data.has("info"));
		assertTrue(data.has("transactions"));
		
		JSONArray transactions = data.getJSONArray("transactions");
		assertTrue(transactions.length() == 3);
	}

	private JSONObject callTheService(JSONObject user) {
		return callTheService(user, 0);
	}
	
	private JSONObject callTheService(JSONObject user, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);
		
		return res.getBody().getObject();
	}

}
