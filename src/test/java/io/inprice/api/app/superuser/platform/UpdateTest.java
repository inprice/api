package io.inprice.api.app.superuser.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of superuser's Platform -> Controller.update(PlatformDTO)
 * 
 * @author mdpinar
 * @since 2021-08-28
 */
@RunWith(JUnit4.class)
public class UpdateTest {

	private static final String SERVICE_ENDPOINT = "/sys/platform";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
				.put("id", 1)
  			.put("name", "Amazon America")
	    	.put("currencyCode", "USD")
				.put("currencyFormat", "$#,##0.00")
				.put("queue", "active.links.queue.cap3")
				.put("profile", "default");

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(SAMPLE_BODY)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Platform_not_found_WITHOUT_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("id");
		
		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Platform not found!", json.getString("reason"));
	}

	@Test
	public void Platform_not_found_WITH_wrong_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", -75);
		
		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Platform not found!", json.getString("reason"));
	}

	@Test
	public void Name_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("name");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Name cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Name_must_be_between_3_and_50_chars_WITH_shorter_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("name", "AB");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Name must be between 3 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Name_must_be_between_3_and_50_chars_WITH_longer_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("name", RandomStringUtils.randomAlphabetic(51));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Name must be between 3 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Currency_Code_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("currencyCode");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Currency Code must be 3 chars!", json.getString("reason"));
	}

	@Test
	public void Currency_Code_must_be_3_chars_WITH_shorter_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("currencyCode", "AB");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Currency Code must be 3 chars!", json.getString("reason"));
	}

	@Test
	public void Currency_Code_must_be_3_chars_WITH_longer_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("currencyCode", "ABCD");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Currency Code must be 3 chars!", json.getString("reason"));
	}


	@Test
	public void Currency_Format_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("currencyFormat");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Currency Format cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Currency_Format_must_be_between_3_and_30_chars_WITH_shorter_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("currencyFormat", "AB");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Currency Format must be between 3 - 30 chars!", json.getString("reason"));
	}

	@Test
	public void Currency_Format_must_be_between_3_and_30_chars_WITH_longer_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("currencyFormat", RandomStringUtils.randomAlphabetic(51));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Currency Format must be between 3 - 30 chars!", json.getString("reason"));
	}

	@Test
	public void Queue_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("queue");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Queue cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Queue_must_be_between_5_and_50_chars_WITH_shorter_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("queue", "ABCD");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Queue must be between 5 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Queue_must_be_between_5_and_50_chars_WITH_longer_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("queue", RandomStringUtils.randomAlphabetic(51));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Queue must be between 5 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Profile_can_be_between_3_and_15_chars_WITH_shorter_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("profile", "AB");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("If given, profile can be between 3 - 15 chars!", json.getString("reason"));
	}

	@Test
	public void Profile_can_be_between_3_and_15_chars_WITH_longer_value() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("profile", RandomStringUtils.randomAlphabetic(51));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("If given, profile can be between 3 - 15 chars!", json.getString("reason"));
	}

	/**
	 * Fulfills the same test for three types of users; viewer, editor and admin.
	 */
	@Test
	public void Forbidden_WITH_normal_users() {
		for (JSONObject user: Fixtures.NORMAL_USER_LIST) {
			JSONObject json = callTheService(user, SAMPLE_BODY);

			assertEquals(403, json.getInt("status"));
  		assertEquals("Forbidden!", json.getString("reason"));
		}
	}

	@Test
	public void Everything_must_be_ok_WITH_superuser() {
		JSONObject json = callTheService(SAMPLE_BODY);

		assertEquals(200, json.getInt("status"));
		assertNotNull("OK", json.getString("reason"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(Fixtures.SUPER_USER, body);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
