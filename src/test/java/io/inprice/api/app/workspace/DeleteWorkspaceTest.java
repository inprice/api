package io.inprice.api.app.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.RandomStringUtils;
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
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of WorkspaceController.deleteWorkspace(admin's password) 
 * 
 * @author mdpinar
 * @since 2021-07-08
 */
@RunWith(JUnit4.class)
public class DeleteWorkspaceTest {

	private static final String SERVICE_ENDPOINT = "/workspace";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}
	
	@Test
	public void Forbidden_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
	}

	@Test
	public void Wrong_password_WITH_empty_password() {
		JSONObject user = TestWorkspaces.Second_professional_plan_and_one_extra_user.ADMIN();
		JSONObject json = callTheServiceWith(user, null);

		assertEquals(112, json.getInt("status"));
		assertNotNull("Wrong password!", json.getString("reason"));
	}

	@Test
	public void Wrong_password_WITH_different_password() {
		JSONObject user = TestWorkspaces.Second_professional_plan_and_one_extra_user.ADMIN();
		JSONObject json = callTheServiceWith(user, "5678");

		assertEquals(112, json.getInt("status"));
		assertNotNull("Wrong password!", json.getString("reason"));
	}

	@Test
	public void Wrong_password_FOR_shorter_password() {
		JSONObject user = TestWorkspaces.Second_professional_plan_and_one_extra_user.ADMIN();
		JSONObject json = callTheServiceWith(user, "123");

		assertEquals(112, json.getInt("status"));
		assertNotNull("Wrong password!", json.getString("reason"));
	}

	@Test
	public void Wrong_password_FOR_longer_password() {
		JSONObject user = TestWorkspaces.Second_professional_plan_and_one_extra_user.ADMIN();
		JSONObject json = callTheServiceWith(user, RandomStringUtils.randomAlphabetic(17));

		assertEquals(112, json.getInt("status"));
		assertNotNull("Wrong password!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_editor() {
		JSONObject user = TestWorkspaces.Second_professional_plan_and_one_extra_user.EDITOR();
		JSONObject json = callTheServiceWith(user, user.getString("password"));

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject user = Fixtures.SUPER_USER;
		JSONObject json = callTheServiceWith(user, user.getString("password"));

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		JSONObject user = TestWorkspaces.Without_a_plan_and_extra_user.ADMIN();
		JSONObject json = callTheServiceWith(user, user.getString("password"));

		assertEquals(200, json.getInt("status"));
	}

	private JSONObject callTheServiceWith(JSONObject user, String password) {
		//login with a viewer
		Cookies cookies = TestUtils.login(user);

		//making service call
		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(new JSONObject().put("value", password))
			.asJson();
		TestUtils.logout(cookies);

		//returning the result to be tested
		return res.getBody().getObject();
	}

}
