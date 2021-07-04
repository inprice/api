package io.inprice.api.app.member;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.app.utils.TestAccount;
import io.inprice.api.app.utils.TestRole;
import io.inprice.api.app.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * 
 * @author mdpinar
 * @since 2021-07-01
 */
@RunWith(JUnit4.class)
public class ListTest {

	private static final String SERVICE_ENDPOINT = "/member";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertNotNull("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer_user() {
		Cookies cookies = TestUtils.login(TestAccount.Standard_plan_and_two_extra_users, TestRole.VIEWER);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_editor_user() {
		Cookies cookies = TestUtils.login(TestAccount.Standard_plan_and_two_extra_users, TestRole.EDITOR);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin_user() {
		Cookies cookies = TestUtils.login(TestAccount.Standard_plan_and_two_extra_users, TestRole.ADMIN);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONArray("data"));
	}

	@Test
	public void Member_not_found_WITH_super_user() {
		Cookies cookies = TestUtils.login(null, TestRole.SUPER);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.header("X-Session", "0")
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertNotNull("Member not found!", json.get("reason"));
	}

}
