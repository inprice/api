package io.inprice.api.app.exim.exbort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
 * Tests the functionality of BrandController.download() under exim
 * 
 * @author mdpinar
 * @since 2021-11-07
 */
@RunWith(JUnit4.class)
public class LinkTest {

	private static final String SERVICE_ENDPOINT = "/exim/link/download";

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
	public void Link_not_found() {
		String csv = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.ADMIN(), "Wrong!");
		assertEquals("Link not found!", csv);
	}

	@Test
	public void Everything_must_be_ok_FOR_any_kind_of_users() {
		Map<TestRoles, JSONObject> roleUserMap = Map.of(
			TestRoles.ADMIN, TestWorkspaces.Standard_plan_and_two_extra_users.ADMIN(),
			TestRoles.EDITOR, TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(),
			TestRoles.VIEWER, TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER()
		);

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			String csv = callTheService(roleUser.getValue(), "Waiting");
			assertNotEquals("Link not found!", csv);
		}
	}

	private String callTheService(JSONObject user, String grup) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<String> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.queryString("grups", grup)
			.asString();
		TestUtils.logout(cookies);

		JSONObject json = null;
		String body = res.getBody();
		
		//checks if result is a json object. if so, there must be problem!
		try {
			json = new JSONObject(body);
		} catch (Exception e) { }
		
		if (json == null) {
			return body;
		} else {
			return json.getString("reason");
		}
	}

}
