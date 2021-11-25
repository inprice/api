package io.inprice.api.app.product;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import io.inprice.api.utils.TestWorkspaces;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of ProductController.setAlarmON/OFF(AlarmEntityDTO)
 * 
 * @author mdpinar
 * @since 2021-11-22
 */
@RunWith(Parameterized.class)
public class AlarmTest {

	private String SERVICE_ENDPOINT = "/product/alarm";

	/**
	 * This method runs this class twice
	 * 
	 */
  @Parameterized.Parameters
  public static Object[][] getHttpMethodParams() {
  	return new Object[][] { { "/on" }, { "/off" } };
  }
  
  public AlarmTest(String postfix) {
  	this.SERVICE_ENDPOINT += postfix;
  }

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(TestWorkspaces.Standard_plan_and_two_extra_users.ADMIN(), null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Product_not_found_WITHOUT_link_id_set() {
		JSONObject json = callTheService(new Long[] {});

		assertEquals(404, json.getInt("status"));
		assertEquals("Product not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, new Long[] { 1L });

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheService(TestWorkspaces.Pro_plan_with_two_extra_users.VIEWER(), new Long[] { 1L }, 0);

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	/**
	 * Consists of six steps;
	 *	a) to gather other workspace's products, admin is logged in
	 *	b) searches some specific products
	 *  c) picks one of those products
	 *  d) builds body up
	 *  e) evil user logs in
	 *  f) tries to delete other workspace's products
	 */
	@Test
	public void Alarm_not_found_WHEN_trying_to_update_someone_elses_products() {
		//to gather other workspace's links, admin is logged in
		Cookies cookies = TestUtils.login(TestWorkspaces.Standard_plan_and_one_extra_user.ADMIN());

		//searches some specific products
		JSONArray productList = TestFinder.searchProducts(cookies, "Product R", 0);
		assertNotNull(productList);
		
		TestUtils.logout(cookies); //here is important!

		//evil user logs in
		cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		//searches for alarms
		JSONArray alarmList = TestFinder.searchAlarms(cookies, "PRODUCT");
		assertNotNull(alarmList);

		//picks one of those products
		Long[] productIds = { productList.getJSONObject(0).getLong("id") };
		
		//builds the body up
		JSONObject body = new JSONObject();
		body.put("alarmId", alarmList.getJSONObject(0).getLong("id"));
		body.put("entityIdSet", productIds);

		//tries to update other users' products
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertEquals("Alarm not found!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 *	a) searches for alarms for products
	 *	b) searches for trying products
	 *  c) builds body up with those product and alarm
	 *  d) tries to set alarm on
	 */
	@Test
	public void You_have_reached_max_alarm_number_of_your_plan() {
		if (SERVICE_ENDPOINT.endsWith("/off")) return;

		//to gather other workspace's products, admin is logged in
		Cookies cookies = TestUtils.login(TestWorkspaces.Basic_plan_but_no_extra_user_for_alarm_limits.ADMIN());

		//searches for alarms
		JSONArray alarmList = TestFinder.searchAlarms(cookies, "PRODUCT");
		assertNotNull(alarmList);

		//searches for trying products (must be 1)
		JSONArray productsList = TestFinder.searchProducts(cookies, "Product O", 0);
		assertNotNull(productsList);
		assertTrue(productsList.length() == 1);

		//picks one of those products
		Long[] productIds = { productsList.getJSONObject(0).getLong("id") };
		
		//builds the body up
		JSONObject body = new JSONObject();
		body.put("alarmId", alarmList.getJSONObject(0).getLong("id"));
		body.put("entityIdSet", productIds);

		//tries to update other users' products
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(910, json.getInt("status"));
		assertEquals("You have reached max alarm number of your plan!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) editor or admin logs in
	 *	b) searches some specific products
	 *  c) gathers two of them
	 *  d) builds body up
	 *  e) deletes those selected products
	 */
	@Test
	public void Everything_must_be_ok_FOR_editor_and_admin() {
		//both workspace have 2 products
		JSONObject[] users = {
			TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN(),
			TestWorkspaces.Starter_plan_and_one_extra_user.EDITOR()
		};

		for (JSONObject user: users) {
			Cookies cookies = TestUtils.login(user);

			//searches for alarms
			JSONArray alarmList = TestFinder.searchAlarms(cookies, "PRODUCT");
  		assertNotNull(alarmList);
  
  		//searches some specific products
  		JSONArray productList = TestFinder.searchProducts(cookies, "Product", 0);
  
  		assertNotNull(productList);
  		assertEquals(2, productList.length());
  
  		//gathers two of them
  		Long[] productIds = new Long[2];
  		
  		for (int i = 0; i < 2; i++) {
  			JSONObject link = productList.getJSONObject(i);
  			productIds[i] = link.getLong("id");
  		}

  		//builds the body up
  		JSONObject body = new JSONObject();
  		body.put("alarmId", alarmList.getJSONObject(0).getLong("id"));
  		body.put("entityIdSet", productIds);
  
  		//deletes those selected products
  		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
  			.headers(Fixtures.SESSION_0_HEADERS)
  			.cookie(cookies)
  			.body(body)
  			.asJson();
  		TestUtils.logout(cookies);
  
  		JSONObject json = res.getBody().getObject();
  		assertEquals(200, json.getInt("status"));
		}
	}

	private JSONObject callTheService(Long[] productIds) {
		return callTheService(TestWorkspaces.Standard_plan_and_no_extra_users.ADMIN(), productIds);
	}

	private JSONObject callTheService(JSONObject user, Long[] entityIdSet) {
		return callTheService(user, entityIdSet, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long[] entityIdSet, int session) {
		Cookies cookies = TestUtils.login(user);
		
		JSONObject body = null;
		if (entityIdSet != null) {
			body = new JSONObject();
			if (entityIdSet != null && entityIdSet.length > 0) body.put("entityIdSet", entityIdSet);
		}

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
