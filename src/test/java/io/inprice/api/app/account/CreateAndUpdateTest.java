package io.inprice.api.app.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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
 * Tests the functionality of AccountService.create(CreateDTO) and also update(CreateDTO) 
 * 
 * This class is equipped with Parameterized runner so that we are able to run the same tests for both create and update! 
 * 
 * @author mdpinar
 * @since 2021-07-08
 */
@RunWith(Parameterized.class)
public class CreateAndUpdateTest {

	private static final String SERVICE_ENDPOINT = "/account";

	private String httpMethod;

	/**
	 * This method runs this class twice!
	 * One for Create with POST http method
	 * And the other is for Update with PUT method
	 * 
	 */
  @Parameterized.Parameters
  public static Object[][] getHttpMethodParams() {
  	return new Object[][] { { "POST" }, { "PUT" } };
  }
  
  public CreateAndUpdateTest(String httpMethod) {
  	this.httpMethod = httpMethod;
  }

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Forbidden_WITH_no_session() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT).asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.get("reason"));
	}

	@Test
	public void Account_name_cannot_be_empty_WITH_empty_name() {
		JSONObject json = callTheServiceWith(null, "USD", "$#,##0.00");

		assertEquals(400, json.getInt("status"));
		assertEquals("Account name cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Account_name_must_be_between_5_and_70_chars_WITH_shorter_name() {
		JSONObject json = callTheServiceWith("Acme", "USD", "$#,##0.00");

		assertEquals(400, json.getInt("status"));
    assertEquals("Account name must be between 5 - 70 chars!", json.getString("reason"));
	}

	@Test
	public void Account_name_must_be_between_5_and_70_chars_WITH_longer_name() {
		JSONObject json = callTheServiceWith(RandomStringUtils.randomAlphabetic(71), "USD", "$#,##0.00");

		assertEquals(400, json.getInt("status"));
		assertEquals("Account name must be between 5 - 70 chars!", json.getString("reason"));
	}

	@Test
	public void Currency_code_cannot_be_empty_WITH_empty_currency_code() {
		JSONObject json = callTheServiceWith("Acme Inc X", null, "$#,##0.00");

		assertEquals(400, json.getInt("status"));
		assertEquals("Currency code cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Currency_code_must_be_3_chars_WITH_shorter_code() {
		JSONObject json = callTheServiceWith("Acme Inc X", "US", "$#,##0.00");

		assertEquals(400, json.getInt("status"));
    assertEquals("Currency code must be 3 chars!", json.getString("reason"));
	}

	@Test
	public void Currency_code_must_be_3_chars_WITH_longer_code() {
		JSONObject json = callTheServiceWith("Acme Inc X", "USDE", "$#,##0.00");

		assertEquals(400, json.getInt("status"));
		assertEquals("Currency code must be 3 chars!", json.getString("reason"));
	}

	@Test
	public void Unknown_currency_code_WITH_undefined_currency() {
		JSONObject json = callTheServiceWith("Acme Inc X", "XYZ", "$#,##0.00");

		assertEquals(400, json.getInt("status"));
    assertEquals("Unknown currency code!", json.getString("reason"));
	}

	@Test
	public void Currency_format_cannot_be_empty_WITH_empty_currency_format() {
		JSONObject json = callTheServiceWith("Acme Inc X", "USD", null);

		assertEquals(400, json.getInt("status"));
		assertEquals("Currency format cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Currency_format_must_be_between_3_and_16_chars_WITH_shorter_format() {
		JSONObject json = callTheServiceWith("Acme Inc X", "USD", "##");

		assertEquals(400, json.getInt("status"));
    assertEquals("Currency format must be between 3 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Currency_format_must_be_between_3_and_16_chars_WITH_longer_format() {
		JSONObject json = callTheServiceWith("Acme Inc X", "USD", StringUtils.repeat('#', 17));

		assertEquals(400, json.getInt("status"));
    assertEquals("Currency format must be between 3 - 16 chars!", json.getString("reason"));
	}

	@Test
	public void Currency_format_is_invalid_WITH_wrong_format() {
		JSONObject json = callTheServiceWith("Acme Inc X", "USD", "£##W");

		assertEquals(400, json.getInt("status"));
    assertTrue(json.getString("reason").startsWith("Currency format is invalid!"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_super_user() {
		JSONObject json = callTheServiceWith(Fixtures.SUPER_USER);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	/**
	 * Please note that a viewer can update the account in which he is the admin when he logs in
	 */
	@Test
	public void Everything_must_be_ok_WITH_viewer() {
		JSONObject json = callTheServiceWith(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		assertEquals(200, json.getInt("status"));
		if (httpMethod.equals("POST")) { //for only create operation!
			assertNotNull(json.getJSONObject("data"));
		}
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		JSONObject json = callTheServiceWith("Acme Inc X", "USD", "$#,##0.00");

		assertEquals(200, json.getInt("status"));
		if (httpMethod.equals("POST")) { //for only create operation!
			assertNotNull(json.getJSONObject("data"));
		}
	}

	private JSONObject callTheServiceWith(JSONObject user) {
		//creating the body
		JSONObject body = new JSONObject();
		body.put("name", "Acme Inc X");
		body.put("currencyCode", "USD");
		body.put("currencyFormat", "$#,##0.00");

		//login with a viewer
		Cookies cookies = TestUtils.login(user);

		//making service call
		HttpResponse<JsonNode> res = Unirest.request(httpMethod, SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		
		//logout
		TestUtils.logout(cookies);

		//returning the result to be tested
		return res.getBody().getObject();
	}

	private JSONObject callTheServiceWith(String name, String currencyCode, String currencyFormat) {
		//creating the body
		JSONObject body = new JSONObject();
		if (name != null) body.put("name", name);
		if (currencyCode != null) body.put("currencyCode", currencyCode);
		if (currencyFormat != null) body.put("currencyFormat", currencyFormat);

		//login with an admin
		Cookies cookies = TestUtils.login(TestAccounts.Basic_plan_but_no_extra_user.ADMIN());

		//making service call
		HttpResponse<JsonNode> res = Unirest.request(httpMethod, SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		
		//logout
		TestUtils.logout(cookies);

		//returning the result to be tested
		return res.getBody().getObject();
	}

}
