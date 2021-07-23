package io.inprice.api.app.superuser.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
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
 * Tests the functionality of superuser's Account -> Controller.createCoupon(CreateCouponDTO)
 * 
 * @author mdpinar
 * @since 2021-07-22
 */
@RunWith(JUnit4.class)
public class CreateCouponTest {

	private static final String SERVICE_ENDPOINT = "/sys/account/coupon";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
  			.put("accountId", 1)
  			.put("planId", 10)
	    	.put("days", 30)
	    	.put("description", "Here is your free coupon.");

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(SAMPLE_BODY)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Account_id_is_missing() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("accountId");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Account id is missing!", json.getString("reason"));
	}

	@Test
	public void Invalid_plan_WITHOUT_planId() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("planId");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Invalid plan!", json.getString("reason"));
	}
	
	@Test
	public void Days_info_is_invalid_WITHOUT_days() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("days");
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Days info is invalid, it must be between 14 - 365!", json.getString("reason"));
	}

	@Test
	public void Days_info_is_invalid_WITH_less_than_14_days() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("days", 13);
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Days info is invalid, it must be between 14 - 365!", json.getString("reason"));
	}

	@Test
	public void Days_info_is_invalid_WITH_greater_than_365_days() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("days", 366);
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Days info is invalid, it must be between 14 - 365!", json.getString("reason"));
	}

	@Test
	public void Description_can_be_up_to_128_chars_WITH_longer_description() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("description", RandomStringUtils.randomAlphabetic(129));

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Description can be up to 128 chars!", json.getString("reason"));
	}
	
	@Test
	public void Plan_not_found_WITH_wrong_planId() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("planId", 999);
		
		JSONObject json = callTheService(body);
		
		assertEquals(404, json.getInt("status"));
		assertEquals("Plan not found!", json.getString("reason"));
	}

	@Test
	public void Account_not_found_WITH_wrong_accountId() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("accountId", 999);

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Account not found!", json.getString("reason"));
	}

	/**
	 * Fulfills the same test for three types of users; viewer, editor and admin.
	 */
	@Test
	public void Forbidden_WITH_normal_users() {
		List<JSONObject> userList = new ArrayList<>(3);
		userList.add(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());
		userList.add(TestAccounts.Starter_plan_and_one_extra_user.EDITOR());
		userList.add(TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		for (JSONObject user: userList) {
			JSONObject json = callTheService(user, SAMPLE_BODY);

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}

	@Test
	public void You_already_have_an_active_subscription() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		JSONArray accountList = TestFinder.searchAccounts(cookies, "With Starter Plan");
		assertNotNull(accountList);
		assertTrue(accountList.length() > 0);
		
		JSONObject account = accountList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("accountId", account.getLong("xid"));

		JSONObject json = callTheService(cookies, body);
		TestUtils.logout(cookies);

		assertEquals(807, json.getInt("status"));
		assertEquals("You already have an active subscription!", json.getString("reason"));
	}

	@Test
	public void Current_limits_of_this_account_are_greater_than_the_plans() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		JSONArray accountList = TestFinder.searchAccounts(cookies, "Cancelled -Starter Plan-");
		assertNotNull(accountList);
		assertTrue(accountList.length() > 0);
		
		JSONObject account = accountList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("accountId", account.getLong("xid"));

		JSONObject json = callTheService(cookies, body);
		TestUtils.logout(cookies);

		assertEquals(400, json.getInt("status"));
		assertEquals("Current limits of this account are greater than the plan's!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		JSONArray accountList = TestFinder.searchAccounts(cookies, "Without A Plan");
		assertNotNull(accountList);
		assertTrue(accountList.length() > 0);
		
		JSONObject account = accountList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("accountId", account.getLong("xid"));

		JSONObject json = callTheService(cookies, body);
		TestUtils.logout(cookies);

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(Fixtures.SUPER_USER, body);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		Cookies cookies = TestUtils.login(user);

		JSONObject json = callTheService(cookies, body);
		TestUtils.logout(cookies);

		return json;
	}
	
	private JSONObject callTheService(Cookies cookies, JSONObject body) {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();

		return res.getBody().getObject();
	}

}
