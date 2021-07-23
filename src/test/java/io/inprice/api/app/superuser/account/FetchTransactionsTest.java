package io.inprice.api.app.superuser.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

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
 * Tests the functionality of superuser's Account -> Controller.fetchTransactions(Long accountId)
 * 
 * @author mdpinar
 * @since 2021-07-23
 */
@RunWith(JUnit4.class)
public class FetchTransactionsTest {

	private static final String SERVICE_ENDPOINT = "/sys/account/details/transactions/{accountId}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("accountId", "1")
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
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
			JSONObject json = callTheService(user, 1L);

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}
	
	@Test
	public void Account_not_found_WITH_non_existing_id() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 999L);
		
		assertEquals(404, json.getInt("status"));
		assertEquals("Account not found!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		JSONArray accountList = TestFinder.searchAccounts(cookies, "With Standard Plan and Two Extra Users");
		assertNotNull(accountList);
		assertEquals(1, accountList.length());
		
		JSONObject account = accountList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.cookie(cookies)
			.routeParam("accountId", ""+account.getLong("xid"))
			.asJson();
		TestUtils.logout(cookies);
		
		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONArray data = json.getJSONArray("data");

		assertEquals(3, data.length());
	}
	
	private JSONObject callTheService(JSONObject user, Long accountId) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.cookie(cookies)
			.routeParam("accountId", (accountId != null ? ""+accountId : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
