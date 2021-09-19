package io.inprice.api.app.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of DashboardController.getReport(true/false)
 *  
 * This class is equipped with Parameterized runner so that we are able to run the same tests while getting cold and hot report! 
 * 
 * @author mdpinar
 * @since 2021-07-10
 */
@RunWith(Parameterized.class)
public class GetAndRefreshReportTests {

	private String SERVICE_ENDPOINT = "/dashboard";

	/**
	 * This method runs this class twice, getReport(true and false)
	 * 
	 */
  @Parameterized.Parameters
  public static Object[][] getHttpMethodParams() {
  	return new Object[][] { { "" }, { "/refresh" } };
  }
  
  public GetAndRefreshReportTests(String postfix) {
  	this.SERVICE_ENDPOINT += postfix;
  }

  @BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Forbidden_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT).asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
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

		assertEquals(915, json.getInt("status"));
		assertNotNull("You must bind an workspace!", json.get("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) binds to first workspace
	 * 	c) gets the report
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_and_bound_workspace() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.put("/sys/workspace/bind/1")
			.cookie(cookies)
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));
		
		res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		JSONObject data = json.getJSONObject("data");
		assertNotNull(data);
		assertTrue(data.has("date"));
		assertTrue(data.has("products"));
		assertTrue(data.has("links"));
		assertTrue(data.has("workspace"));
	}

}
