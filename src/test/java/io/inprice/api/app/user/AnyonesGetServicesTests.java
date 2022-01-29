package io.inprice.api.app.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
 * Tests the functionality of UserService methods (accessible by anyone) getInvitations(), getMemberships() and getOpenedSessions()
 * 
 * This class is equipped with Parameterized runner so that we are able to run the same tests for the three functions mentioned above! 
 * 
 * @author mdpinar
 * @since 2021-07-10
 */
@RunWith(Parameterized.class)
public class AnyonesGetServicesTests {

	private String SERVICE_ENDPOINT = "/user";
	private String endpointPostfix;

	/**
	 * This method runs this class thrice, getInvitations, getMemberships and getOpenedSessions()
	 * 
	 */
  @Parameterized.Parameters
  public static Object[][] getHttpMethodParams() {
  	return new Object[][] { { "/invitations" }, { "/memberships" }, { "/opened-sessions" } };
  }
  
  public AnyonesGetServicesTests(String postfix) {
  	this.SERVICE_ENDPOINT += postfix;
  	this.endpointPostfix = postfix;
  }

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
	public void You_must_bind_an_workspace_WITH_superuser_and_no_binding() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertFalse(json.has("data"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) binds to first workspace
	 * 	c) gets membership list
	 */
	@Test
	public void Data_must_be_empty_WITH_superuser_and_bound_workspace() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.put("/sys/workspace/bind/1")
			.cookie(cookies)
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(SERVICE_ENDPOINT, 200, json.getInt("status"));
		
		res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));
		assertFalse(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_WITH_viewer_user() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS) //this user has more than one role. he is VIEWER in his second session!
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");
		
		Map<String, Integer> countsMap = Map.of("/memberships", 3, "/invitations", 1, "/opened-sessions", 1);

		assertEquals(200, json.getInt("status"));
		assertNotNull(SERVICE_ENDPOINT, data);
		assertEquals(SERVICE_ENDPOINT, countsMap.get(endpointPostfix).intValue(), data.length());
	}

}
