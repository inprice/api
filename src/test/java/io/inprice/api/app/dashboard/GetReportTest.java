package io.inprice.api.app.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of DashboardService.getReport(true/false)
 *  
 * This class is equipped with Parameterized runner so that we are able to run the same while getting cold and hot report! 
 * 
 * @author mdpinar
 * @since 2021-07-10
 */
@RunWith(Parameterized.class)
public class GetReportTest {

	private String SERVICE_ENDPOINT = "/dashboard";

	/**
	 * This method runs this class twice, getReport(true and false)
	 * 
	 */
  @Parameterized.Parameters
  public static Object[][] getHttpMethodParams() {
  	return new Object[][] { { "" }, { "/refresh" } };
  }
  
  public GetReportTest(String SERVICE_ENDPOINT) {
  	this.SERVICE_ENDPOINT += SERVICE_ENDPOINT;
  }

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Forbidden_WITH_no_session() {
		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT).asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
	}

	@Test
	public void Everything_must_be_ok() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		JSONObject data = json.getJSONObject("data");
		assertNotNull(data);
		assertTrue(data.has("date"));
		assertTrue(data.has("groups"));
		assertTrue(data.has("links"));
		assertTrue(data.has("account"));
	}

}
