package io.inprice.api.app.category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestRoles;
import io.inprice.api.utils.TestUtils;
import io.inprice.api.utils.TestWorkspaces;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of CategoryController.list()
 * 
 * @author mdpinar
 * @since 2021-09-19
 */
@RunWith(JUnit4.class)
public class ListTest {

	private static final String SERVICE_ENDPOINT = "/def/category/list";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}


	@Test
	public void Everything_must_be_ok_WITH_anyone() {
		Map<TestRoles, JSONObject> roleUserMap = Map.of(
			TestRoles.VIEWER, TestWorkspaces.Second_Premium_plan_and_two_extra_users.VIEWER(),
			TestRoles.EDITOR, TestWorkspaces.Second_Premium_plan_and_two_extra_users.EDITOR(),
			TestRoles.ADMIN, TestWorkspaces.Second_Premium_plan_and_two_extra_users.ADMIN()
		);

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue());
  		assertEquals(200, json.getInt("status"));
  		assertTrue(json.has("data"));
  		assertTrue(json.getJSONArray("data").length() == 4);
		}
	}

	private JSONObject callTheService(JSONObject user) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
