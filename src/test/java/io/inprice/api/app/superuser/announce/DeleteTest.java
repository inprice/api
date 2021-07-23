package io.inprice.api.app.superuser.announce;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

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
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of superuser's Announce -> Controller.delete(Long announceId)
 * 
 * @author mdpinar
 * @since 2021-07-23
 */
@RunWith(JUnit4.class)
public class DeleteTest {

	private static final String SERVICE_ENDPOINT = "/sys/announce/{id}";

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
	public void Page_not_found_WITHOUT_id() {
		JSONObject json = callTheService(null);

		assertEquals(404, json.getInt("status"));
    assertEquals("Page not found!", json.getString("reason"));
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
	public void Announce_not_found_WITH_wrong_id() {
		JSONObject json = callTheService(999L);

		assertEquals(404, json.getInt("status"));
    assertEquals("Announce not found!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private JSONObject callTheService(Long id) {
		return callTheService(Fixtures.SUPER_USER, id);
	}
	
	private JSONObject callTheService(JSONObject user, Long id) {
		Cookies cookies = TestUtils.login(user);

		JSONObject json = callTheService(cookies, id);
		TestUtils.logout(cookies);

		return json;
	}

	private JSONObject callTheService(Cookies cookies, Long id) {
		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+(id != null ? id : ""))
			.asJson();

		return res.getBody().getObject();
	}

}
