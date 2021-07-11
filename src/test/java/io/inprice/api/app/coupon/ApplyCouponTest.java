package io.inprice.api.app.coupon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestCoupons;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of CouponService.applyCoupon(String code) 
 * 
 * @author mdpinar
 * @since 2021-07-09
 */
@RunWith(JUnit4.class)
public class ApplyCouponTest {

	private static final String SERVICE_ENDPOINT = "/coupon/apply/{code}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITH_no_session() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.routeParam("code", "XYZ-1234")
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Forbidden_WITH_editor() {
		JSONObject json = callTheServiceWith(
			TestAccounts.Standard_plan_and_one_extra_user.EDITOR(),
			TestCoupons.AVAILABLE_FOR_BASIC_PLAN_1
		);

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheServiceWith(
			TestAccounts.Standard_plan_and_two_extra_users.VIEWER(),
			TestCoupons.AVAILABLE_FOR_BASIC_PLAN_1
		);

		assertEquals(403, json.getInt("status"));
		assertNotNull("Forbidden!", json.getString("reason"));
	}

	@Test
	public void Page_not_found_WITH_no_code() {
		JSONObject json = callTheServiceWith(TestAccounts.Without_a_plan_and_extra_user.ADMIN(), "");

		assertEquals(404, json.getInt("status"));
		assertNotNull("Page not found!", json.getString("reason"));
	}

	@Test
	public void Invalid_coupon_WITH_empty_code() {
		JSONObject json = callTheServiceWith(TestAccounts.Without_a_plan_and_extra_user.ADMIN(), " ");

		assertEquals(145, json.getInt("status"));
		assertNotNull("Invalid coupon!", json.getString("reason"));
	}

	@Test
	public void Invalid_coupon_WITH_wrong_code() {
		JSONObject json = callTheServiceWith(TestAccounts.Without_a_plan_and_extra_user.ADMIN(), "XYZ-1234");

		assertEquals(145, json.getInt("status"));
		assertNotNull("Invalid coupon!", json.getString("reason"));
	}

	@Test
	public void Coupon_not_found_FOR_not_registered_code() {
		JSONObject json = callTheServiceWith(
			TestAccounts.Without_a_plan_and_extra_user.ADMIN(),
			TestCoupons.NOT_REGISTERED
		);

		assertEquals(404, json.getInt("status"));
		assertNotNull("Coupon not found!", json.getString("reason"));
	}

	@Test
	public void This_coupon_is_already_used_WITH_used_code() {
		JSONObject json = callTheServiceWith(
			TestAccounts.Without_a_plan_and_extra_user.ADMIN(),
			TestCoupons.ALREADY_USED
		);

		assertEquals(804, json.getInt("status"));
		assertNotNull("This coupon is already used!", json.getString("reason"));
	}

	@Test
	public void This_coupon_is_issued_for_another_account_WITH_assigned_code() {
		JSONObject json = callTheServiceWith(
			TestAccounts.Without_a_plan_and_extra_user.ADMIN(),
			TestCoupons.ASSIGNED_BUT_NOT_USED
		);

		assertEquals(702, json.getInt("status"));
		assertNotNull("This coupon is issued for another account!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_super_user() {
		JSONObject json = callTheServiceWith(
			Fixtures.SUPER_USER,
			TestCoupons.ALREADY_USED
		);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void You_already_have_an_active_subscription_WITH_assigned_code() {
		JSONObject json = callTheServiceWith(
			TestAccounts.Standard_plan_and_no_extra_users.ADMIN(),
			TestCoupons.AVAILABLE_FOR_BASIC_PLAN_1
		);

		assertEquals(807, json.getInt("status"));
		assertNotNull("You already have an active subscription!", json.getString("reason"));
	}

	@Test
	public void You_need_to_select_a_broader_plan() {
		JSONObject json = callTheServiceWith(
			TestAccounts.Cancelled_Starter_plan_30_links_6_alarms.ADMIN(),
			TestCoupons.AVAILABLE_FOR_BASIC_PLAN_1
		);

 		assertNotNull("You need to select a broader plan since your actual plan has more permission!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok() {
		JSONObject json = callTheServiceWith(
			TestAccounts.Cancelled_Basic_plan_no_link_no_alarm.ADMIN(),
			TestCoupons.AVAILABLE_FOR_BASIC_PLAN_1
		);

		assertEquals(200, json.getInt("status"));
		assertNotNull(json.getJSONObject("data"));
	}

	private JSONObject callTheServiceWith(JSONObject user, String couponCode) {
		//login with given user
		Cookies cookies = TestUtils.login(user);

		//making service call
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("code", couponCode)
			.asJson();
		
		//logout
		TestUtils.logout(cookies);

		//returning the result to be tested
		return res.getBody().getObject();
	}

}
