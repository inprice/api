package io.inprice.api.app.system;

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
 * Tests the functionality of SystemService.getPlans() 
 * 
 * @author mdpinar
 * @since 2021-07-01
 */
@RunWith(JUnit4.class)
public class GetPlansTest {

	private static final String SERVICE_ENDPOINT = "/app/plans";

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
		Cookies cookies = TestUtils.login(TestAccounts.Without_a_plan_and_extra_user.ADMIN());

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
	}

}
