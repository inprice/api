package io.inprice.api.app.membership;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of MembershipService.resume(Long memberId)
 * 
 * @author mdpinar
 * @since 2021-07-06
 */
@RunWith(JUnit4.class)
public class ResumeTest {

	private static final String SERVICE_ENDPOINT = "/membership/resume/{id}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("id", "1")
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertNotNull("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", "1")
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_editor_user() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.EDITOR());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", "1")
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.get("reason"));
	}

	@Test
	public void Member_not_found_WITH_wrong_id() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", "1")
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(404, json.getInt("status"));
		assertNotNull("Member not found!", json.get("reason"));
	}

	@Test
	public void This_member_is_not_paused_WITH_a_normal_member() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());
		
		Long memberId = findMemberIdByIndex(cookies, 0);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
				.headers(Fixtures.SESSION_0_HEADERS)
				.cookie(cookies)
				.routeParam("id", memberId.toString())
				.asJson();

		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(400, json.getInt("status"));
		assertEquals("This member is not paused!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", "1")
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(511, json.getInt("status"));
		assertNotNull("You are not allowed to do this operation!", json.get("reason"));
	}

	/**
	 * Consists of three steps;
	 *    a) find the second member and delete him
	 *    b) delete him
	 *    c) try to resume
	 */
	@Test
	public void This_member_is_already_deleted_FOR_a_deleted_member() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//finding the second member
		Long memberId = findMemberIdByIndex(cookies, 1); //attention pls!

		//deleting him
		HttpResponse<JsonNode> res = Unirest.delete("/membership/{id}")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", memberId.toString())
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		//better wait some time before second call!
		try {
			Thread.sleep(250);
		} catch (InterruptedException ignored) {}
		
		//try to change member's role
		res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", memberId.toString())
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(802, json.getInt("status"));
    assertEquals("This member is already deleted!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_a_paused_member() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());
		
		Long memberId = findMemberIdByIndex(cookies, 0);

		HttpResponse<JsonNode> res = Unirest.put("/membership/pause/{id}")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", memberId.toString())
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		res = Unirest.put(SERVICE_ENDPOINT)
				.headers(Fixtures.SESSION_0_HEADERS)
				.cookie(cookies)
				.routeParam("id", memberId.toString())
				.asJson();

		TestUtils.logout(cookies);

		json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private Long findMemberIdByIndex(Cookies cookies, int index) {
		HttpResponse<JsonNode> res = Unirest.get("/membership")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");
		
		return data.getJSONObject(index).getLong("id");
	}

}
