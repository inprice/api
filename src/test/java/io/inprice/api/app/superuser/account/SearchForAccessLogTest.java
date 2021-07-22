package io.inprice.api.app.superuser.account;

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
import io.inprice.api.utils.TestRoles;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of superuser's Account -> Controller.searchForAccessLog(ALSearchDTO)
 * 
 * @author mdpinar
 * @since 2021-07-22
 */
@RunWith(JUnit4.class)
public class SearchForAccessLogTest {

	private static final String SERVICE_ENDPOINT = "/sys/account/search-logs";

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

	/**
	 * Fulfills the same test for three types of users; viewer, editor and admin.
	 */
	@Test
	public void Forbidden_WITH_normal_users() {
		Map<TestRoles, JSONObject> roleUserMap = new HashMap<>(3);
		roleUserMap.put(TestRoles.VIEWER, TestAccounts.Standard_plan_and_two_extra_users.VIEWER());
		roleUserMap.put(TestRoles.EDITOR, TestAccounts.Starter_plan_and_one_extra_user.EDITOR());
		roleUserMap.put(TestRoles.ADMIN, TestAccounts.Starter_plan_and_one_extra_user.ADMIN());

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue(), new JSONObject(), (TestRoles.VIEWER.equals(roleUser.getKey()) ? 1 : 0));

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}
	
	@Test
	public void Account_id_is_missing_WITHOUT() {
		JSONObject body = new JSONObject();
		body.put("method", "POST");
		
		JSONObject json = callTheService(Fixtures.SUPER_USER, body);
		assertEquals(400, json.getInt("status"));
		assertEquals("Account id is missing!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject body = new JSONObject();
		body.put("accountId", 1);
		body.put("method", "POST");

		JSONObject json = callTheService(Fixtures.SUPER_USER, body);
		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
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
