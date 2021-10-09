package io.inprice.api.app.announce;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of AnnounceController.search(SearchDTO)
 * 
 * @author mdpinar
 * @since 2021-07-15
 */
@RunWith(JUnit4.class)
public class SearchTest {

	private static final String SERVICE_ENDPOINT = "/announces/search";

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
	public void You_must_bind_an_workspace_WITH_superuser_WITHOUT_binding_workspace() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, createBody(null));

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an workspace!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) binds to first workspace
	 * 	c) gets announce list (possibly empty)
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_workspace() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.put("/sys/workspace/bind/1")
			.cookie(cookies)
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));
		
		res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(new JSONObject())
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_FOR_user() {
		JSONObject json = callTheService(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN(), createBody(new String[] { "USER" }));
		assertEquals(200, json.getInt("status"));

		JSONArray rows = json.getJSONArray("data");
		assertEquals(1, rows.length());
	}

	@Test
	public void Everything_must_be_ok_FOR_workspace() {
		JSONObject json = callTheService(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN(), createBody(new String[] { "WORKSPACE" }));
		assertEquals(200, json.getInt("status"));

		JSONArray rows = json.getJSONArray("data");
		assertEquals(1, rows.length());
	}

	@Test
	public void Everything_must_be_ok_FOR_system() {
		JSONObject json = callTheService(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN(), createBody(new String[] { "SYSTEM" }));
		assertEquals(200, json.getInt("status"));

		JSONArray rows = json.getJSONArray("data");
		assertEquals(1, rows.length());
	}

	@Test
	public void Everything_must_be_ok_WITH_mixin() {
		JSONObject json = callTheService(TestWorkspaces.Without_a_plan_and_extra_user.ADMIN(), createBody(new String[] { "SYSTEM", "WORKSPACE", "USER" }));
		assertEquals(200, json.getInt("status"));

		JSONArray rows = json.getJSONArray("data");
		assertEquals(3, rows.length());
	}

	private JSONObject createBody(String[] types) {
		JSONObject body = new JSONObject();
		if (types != null) body.put("types", types);

		return body;
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
