package io.inprice.api.app.subscription;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
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
 * Tests the functionality of SubscriptionController.saveInfo(CustomerDTO)
 * 
 * @author mdpinar
 * @since 2021-07-18
 */
@RunWith(JUnit4.class)
public class SaveInfoTest {

	private static final String SERVICE_ENDPOINT = "/subscription/save-info";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
  			.put("title", "Acme in Detroit Inc.")
  			.put("contactName", "Henry Parker")
  			.put("taxId", "123")
	    	.put("taxOffice", "Green Wood")
	    	.put("address1", "Mulholand Drive")
	    	.put("address2", "Number 4")
	    	.put("postcode", "34420")
	    	.put("city", "Bay City")
	    	.put("state", "Michigan")
	    	.put("country", "United States");

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
	public void Company_Name_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("title");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Company Name cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Company_Name_must_be_between_3_and_255_chars_WITH_shorter_title() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("title", "AB");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Company Name must be between 3 - 255 chars!", json.getString("reason"));
	}

	@Test
	public void Company_Name_must_be_between_3_and_255_chars_WITH_longer_title() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("title", RandomStringUtils.randomAlphabetic(256));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Company Name must be between 3 - 255 chars!", json.getString("reason"));
	}

	@Test
	public void Address_line_1_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("address1");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Address line 1 cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Address_line_1_can_be_up_to_255_chars_WITH_longer_address() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("address1", RandomStringUtils.randomAlphabetic(256));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Address line 1 can be up to 255 chars!", json.getString("reason"));
	}

	@Test
	public void City_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("city");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("City cannot be empty!", json.getString("reason"));
	}

	@Test
	public void City_must_be_between_2_and_50_chars_WITH_shorter_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("city", "A");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("City must be between 2 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void City_must_be_between_2_and_50_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("city", RandomStringUtils.randomAlphabetic(51));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("City must be between 2 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Country_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("country");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Country cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Country_must_be_between_3_and_50_chars_WITH_shorter_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("country", "AB");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Country must be between 3 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Country_must_be_between_3_and_50_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("country", RandomStringUtils.randomAlphabetic(51));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Country must be between 3 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Contact_Name_can_be_up_to_50_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("contactName", RandomStringUtils.randomAlphabetic(51));

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Contact Name can be up to 50 chars!", json.getString("reason"));
	}

	@Test
	public void Tax_Id_can_be_up_to_8_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("taxId", RandomStringUtils.randomAlphabetic(17));

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Tax Id can be up to 16 chars!", json.getString("reason"));
	}

	@Test
	public void Tax_Office_can_be_up_to_8_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("taxOffice", RandomStringUtils.randomAlphabetic(26));

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Tax Office can be up to 25 chars!", json.getString("reason"));
	}

	@Test
	public void Address_line_2_can_be_up_to_255_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("address2", RandomStringUtils.randomAlphabetic(256));

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Address line 2 can be up to 255 chars!", json.getString("reason"));
	}

	@Test
	public void Postcode_can_be_up_to_8_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("postcode", RandomStringUtils.randomAlphabetic(9));

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Postcode can be up to 8 chars!", json.getString("reason"));
	}

	@Test
	public void State_can_be_up_to_50_chars_WITH_shorter_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("state", RandomStringUtils.randomAlphabetic(51));

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("State can be up to 50 chars!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, SAMPLE_BODY);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_editor() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.EDITOR());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(SAMPLE_BODY)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_1_HEADERS) //attention pls!
			.cookie(cookies)
			.body(SAMPLE_BODY)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.ADMIN());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(SAMPLE_BODY)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestWorkspaces.Premium_plan_with_no_user.ADMIN(), body);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
