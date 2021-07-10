package io.inprice.api.app.coupon;

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
 * Tests the functionality of CouponService.getCoupons() 
 * 
 * @author mdpinar
 * @since 2021-07-08
 */
@RunWith(JUnit4.class)
public class GetCouponsTest {

	private static final String SERVICE_ENDPOINT = "/coupon";

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
	public void Coupon_not_found_FOR_no_coupon_account() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.VIEWER());

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(404, json.getInt("status"));
		assertEquals("Coupon not found!", json.get("reason"));
	}

	@Test
	public void You_must_bind_an_account_FOR_super_user_without_account_binding() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_O_HEADERS)
			.cookie(cookies)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		
		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an account!", json.get("reason"));
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
		assertEquals(1, data.length());
	}

}
