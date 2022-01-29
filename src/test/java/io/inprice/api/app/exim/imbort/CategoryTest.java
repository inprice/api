package io.inprice.api.app.exim.imbort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
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
 * Tests the functionality of CategoryController.upload(String csvContent) under exim
 * 
 * @author mdpinar
 * @since 2021-11-07
 */
@RunWith(JUnit4.class)
public class CategoryTest {

	private static final String SERVICE_ENDPOINT = "/exim/category/upload";
	
	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body("empty.csv")
			.asJson();

		JSONObject json = res.getBody().getObject();

		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheService(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER(), "empty.csv");

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_dont_have_an_active_plan() {
		JSONObject json = callTheService(TestWorkspaces.Cancelled_Standard_plan.ADMIN(), "categories_2.csv");

		assertEquals(903, json.getInt("status"));
		assertEquals("You don't have an active plan!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_FOR_any_kind_of_users() {
		Map<TestRoles, JSONObject> roleUserMap = Map.of(
			TestRoles.ADMIN, TestWorkspaces.Premium_plan_and_two_extra_users.ADMIN(),
			TestRoles.EDITOR, TestWorkspaces.Premium_plan_and_two_extra_users.EDITOR()
		);

		Map<TestRoles, String> roleFileMap = Map.of(
			TestRoles.ADMIN, "categories_1.csv",
			TestRoles.EDITOR, "categories_2.csv"
		);
		
		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue(), roleFileMap.get(roleUser.getKey()));

			assertEquals(roleUser.getKey().name(), 200, json.getInt("status"));
			
			JSONObject data = json.getJSONObject("data");
			assertEquals(data.getInt("total"), data.getInt("successCount"));
		}
	}

	private JSONObject callTheService(JSONObject user, String fileName) {
		Cookies cookies = TestUtils.login(user);
		
		String body = null;
		try {
			body = IOUtils.toString(this.getClass().getResourceAsStream("/files/"+fileName), StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.contentType("text/csv")
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
