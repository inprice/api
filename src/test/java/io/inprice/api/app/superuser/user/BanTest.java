package io.inprice.api.app.superuser.user;

import static org.junit.Assert.assertEquals;

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
 * Tests the functionality of superuser's User -> Controller.ban(IdTextDTO)
 * 
 * @author mdpinar
 * @since 2021-07-24
 */
@RunWith(JUnit4.class)
public class BanTest {

	private static final String SERVICE_ENDPOINT = "/sys/user/ban";

	private static final long SUPERUSER_ID = 1L;
	private static final long BANNED_USER_ID = 2L;
	
	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
  			.put("id", 3)
	    	.put("text", "Abusive user.");

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
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
	public void Missing_user_id_WITH_wrong_user_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", 0);

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Missing user id!", json.getString("reason"));
	}
	
	@Test
	public void You_cannot_ban_yourself() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", SUPERUSER_ID);
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("You cannot ban yourself!", json.getString("reason"));
	}
	
	@Test
	public void User_is_already_banned() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", BANNED_USER_ID);
		
		JSONObject json = callTheService(body);
		
		assertEquals(825, json.getInt("status"));
		assertEquals("User is already banned!", json.getString("reason"));
	}

	@Test
	public void User_not_found() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", 999);

		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("User not found!", json.getString("reason"));
	}
	
	@Test
	public void Reason_must_be_between_5_128_chars_WITHOUT_text() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("text");
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Reason must be between 5 - 128 chars!", json.getString("reason"));
	}
	
	@Test
	public void Reason_must_be_between_5_128_chars_WITH_shorter_text() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("text", "AB12");
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Reason must be between 5 - 128 chars!", json.getString("reason"));
	}

	@Test
	public void Reason_must_be_between_5_128_chars_WITH_longer_text() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("text", RandomStringUtils.randomAlphabetic(129));

		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Reason must be between 5 - 128 chars!", json.getString("reason"));
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
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", 3);

		JSONObject json = callTheService(body);

		assertEquals(200, json.getInt("status"));
		assertEquals(true, json.getBoolean("ok"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(Fixtures.SUPER_USER, body);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		Cookies cookies = TestUtils.login(user);

		JSONObject json = callTheService(cookies, body);
		TestUtils.logout(cookies);

		return json;
	}
	
	private JSONObject callTheService(Cookies cookies, JSONObject body) {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();

		return res.getBody().getObject();
	}

}
