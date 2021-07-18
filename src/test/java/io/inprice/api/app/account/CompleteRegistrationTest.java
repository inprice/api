package io.inprice.api.app.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestUtils;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of AccountController.completeRegistration(String token) 
 * 
 * @author mdpinar
 * @since 2021-07-06
 */
@RunWith(JUnit4.class)
public class CompleteRegistrationTest {

	private static final String SERVICE_ENDPOINT = "/complete-registration";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Invalid_token_WITH_empty_token() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(130, json.getInt("status"));
		assertEquals("Invalid token!", json.get("reason"));
	}

	@Test
	public void Invalid_token_WITHOUT_existing_token() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.queryString("token", "XYZ-123")
			.asJson();

		JSONObject json = res.getBody().getObject();
		
		assertEquals(130, json.getInt("status"));
		assertEquals("Invalid token!", json.get("reason"));
	}

	@Test
	public void Everything_must_be_ok() {
		JSONObject body = new JSONObject()
			.put("accountName", "Acme X Inc.")
			.put("email", "user-99@acme-x.com")
    	.put("password", "1234")
    	.put("repeatPassword", "1234");

		HttpResponse<JsonNode> res = Unirest.post("/request-registration")
			.body(body)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		
		res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.queryString("token", data.getString("token").replace("-", ""))
			.asJson();
		TestUtils.logout(res.getCookies());

		json = res.getBody().getObject();
		data = json.getJSONObject("data");
		
		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		
	}

}
