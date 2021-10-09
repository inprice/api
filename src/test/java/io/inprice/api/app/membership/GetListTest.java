package io.inprice.api.app.membership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
 * Tests the functionality of MembershipController.getList()
 * 
 * @author mdpinar
 * @since 2021-07-01
 */
@RunWith(JUnit4.class)
public class GetListTest {

	private static final String SERVICE_ENDPOINT = "/membership";

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
		assertNotNull("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer_user() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_editor_user() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR());

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin_user() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_two_extra_users.ADMIN());

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		assertEquals(2, data.length());
	}

	@Test
	public void Member_not_found_WITH_superuser() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertNotNull("Member not found!", json.get("reason"));
	}

}
