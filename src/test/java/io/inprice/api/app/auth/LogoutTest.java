package io.inprice.api.app.auth;

import static org.junit.Assert.assertEquals;

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
 * Tests the functionality of AuthController.logout()
 * 
 * @author mdpinar
 * @since 2021-07-01
 */
@RunWith(JUnit4.class)
public class LogoutTest {

	private static final String SERVICE_ENDPOINT = "/logout";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}
	
	@Test
	public void Seems_that_you_are_already_logged_out_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(801, json.getInt("status"));
		assertEquals("Seems that you are already logged out!", json.getString("reason"));
	}
	
	@Test
	public void Seems_that_you_are_already_logged_out_FOR_multiple_logout() {
		//in order to get a valid cookied, user logins and logsout first
		Cookies cookies = TestUtils.login(TestWorkspaces.Second_professional_plan_and_one_extra_user.EDITOR());
		TestUtils.logout(cookies);
		
		//second logout
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).cookie(cookies).asJson();
		JSONObject json = res.getBody().getObject();
		
		assertEquals(801, json.getInt("status"));
		assertEquals("Seems that you are already logged out!", json.getString("reason"));
	}
	
	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		//in order to get a valid cookie, super user logins first
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);
		
		//handled cookie is used here
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.cookie(cookies)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITHOUT_viewer_login() {
		//in order to get a valid cookie, user logins first
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());
		
		//handled cookie is used here
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).cookie(cookies).asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(200, json.getInt("status"));
    assertEquals("OK", json.getString("reason"));
	}

}
