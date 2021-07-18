package io.inprice.api.app.system;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of SystemController.getStatistics() 
 * 
 * @author mdpinar
 * @since 2021-07-10
 */
@RunWith(JUnit4.class)
public class GetStatisticsTest {

	private static final String SERVICE_ENDPOINT = "/app/statistics";

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
	public void Everything_must_be_ok() {
		final JSONObject user = TestAccounts.Standard_plan_and_two_extra_users.VIEWER();
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		JSONObject data = json.getJSONObject("data");
		assertNotNull(data);
		assertTrue(data.has("userLimit"));
		assertTrue(data.has("linkLimit"));
		assertTrue(data.has("alarmLimit"));
	}

}
