package io.inprice.api.app.smartprice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestUtils;
import io.inprice.api.utils.TestWorkspaces;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of SmartPriceController.test(SmartPriceDTO)
 * 
 * Formula variables;
 * 	p --> Product Price
 *  i --> Minimum Price
 *  a --> Average Price
 *  x --> Maximum Price
 * 
 * Please note that field validations are purposefuly ignored here since they are held in InsertTest!
 * 
 * @author mdpinar
 * @since 2021-10-19
 */
@RunWith(JUnit4.class)
public class TestTest {

	private static final String SERVICE_ENDPOINT = "/smart-price/test";

	private static final JSONObject SAMPLE_BODY = new JSONObject()
			.put("name", "Suitable and in-limit formula")
			.put("formula", "(p*1.10)+0.75")
			.put("lowerLimitFormula", "p/2")
			.put("upperLimitFormula", "(x+i+p)/2")
			;
	
	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Everything_must_be_ok_WITH_correct_formulas() {
		JSONObject json = callTheService(SAMPLE_BODY);
		
		assertEquals(200, json.getInt("status"));
		assertTrue(json.getBoolean("ok"));
		
		JSONArray data = json.getJSONArray("data");
		assertTrue(data.length() == 7);
		assertTrue(data.getJSONObject(0).getDouble("value") == 55.75);
		assertTrue(data.getJSONObject(3).getDouble("value") == 220.75);
		assertTrue(data.getJSONObject(6).getDouble("value") == 375.0);
	}

	@Test
	public void Invalid_formula_WITH_wrong_main_formula() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("formula", "p*1.10)+0.75");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Invalid formula", json.getString("reason"));
	}

	@Test
	public void Invalid_formula_WITH_wrong_lower_limit_formula() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("lowerLimitFormula", "p*1.10)+0.75");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Lower limit: Invalid formula", json.getString("reason"));
	}

	@Test
	public void Division_by_zero_FOR_upper_limit_formula() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("upperLimitFormula", "(p*1.10)/0");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Upper limit: Division by zero!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITHOUT_limiting_formulas() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("lowerLimitFormula");
		body.remove("upperLimitFormula");

		JSONObject json = callTheService(body);
		
		assertEquals(200, json.getInt("status"));
		assertTrue(json.getBoolean("ok"));
	}

	private JSONObject callTheService(JSONObject body) {
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR());

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
