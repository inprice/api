package io.inprice.api.app.link;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of LinkController.moveTo(LinkMoveDTO)
 * 
 * @author mdpinar
 * @since 2021-07-19
 */
@RunWith(JUnit4.class)
public class MoveToTest {

	private static final String SERVICE_ENDPOINT = "/link/move";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.body(new JSONObject())
			.asJson();
		
		JSONObject json = res.getBody().getObject();
		
		assertEquals(401, json.getInt("status"));
		assertEquals("No active session, please sign in!", json.get("reason"));
	}

	@Test
	public void Request_body_is_invalid_WITHOUT_body() {
		JSONObject json = callTheService(null, null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	@Test
	public void Link_not_found_WITHOUT_link_id_set() {
		JSONObject json = callTheService(1L, null);

		assertEquals(404, json.getInt("status"));
		assertEquals("Link not found!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L, new Long[] { 1L });

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		//this user has two roles; one is admin and the other is viewer. so, we need to specify the session number as second to pick viewer session!
		JSONObject json = callTheService(TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER(), 1L, new Long[] { 1L }, 0); //attention!

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	/**
	 * Consists of six steps;
	 *	a) to gather other workspace's links, admin is logged in
	 *	b) searches some specific links
	 *  c) picks one of those links
	 *  d) builds body up
	 *  e) evil user logs in
	 *  f) tries to move other workspace's links
	 */
	@Test
	public void Link_not_found_WHEN_trying_to_move_someone_elses_links() {
		//to gather other workspace's links, admin is logged in
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.ADMIN());

		//searches some specific links
		JSONArray linkList = TestFinder.searchLinks(cookies, "ACTIVE");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(linkList);

		//picks one of those links
		JSONObject link = linkList.getJSONObject(0);
		Long[] linkIds = { link.getLong("id") };
		Long toProductId = findToProductId(TestWorkspaces.Standard_plan_and_no_extra_user.ADMIN(), "Product 2 of Workspace-B");

		//evil user logs in
		cookies = TestUtils.login(TestWorkspaces.Second_professional_plan_and_one_extra_user.EDITOR());

		//builds the body up
		JSONObject body = new JSONObject();
		body.put("toProductId", toProductId);
		body.put("linkIdSet", linkIds);

		//tries to move other workspace's links
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(404, json.getInt("status"));
		assertEquals("Link not found!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) editor or admin logs in
	 *	b) searches some specific links
	 *  c) gathers two of them
	 *  d) builds body up
	 *  e) deletes those selected links
	 */
	@Test
	public void Invalid_product_FOR_null_product_id() {
		//a user logs in
		Cookies cookies = TestUtils.login(TestWorkspaces.Second_professional_plan_and_one_extra_user.EDITOR());

		//searches some specific links
		JSONArray linkList = TestFinder.searchLinks(cookies, "TRYING", 1);

		assertNotNull(linkList);

		//gathers two of them
		Long[] linkIds = new Long[2];
		
		for (int i = 0; i < linkList.length(); i++) {
			JSONObject link = linkList.getJSONObject(i);
			linkIds[i] = link.getLong("id");
		}

		//builds the body up
		JSONObject body = new JSONObject();
		body.put("toProductName", "This is a new product to move.");
		body.put("linkIdSet", linkIds);
		
		//moves those selected links
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(120, json.getInt("status"));
		assertEquals("Invalid product!", json.getString("reason"));
	}

	/**
	 * Consists of five steps;
	 *	a) editor or admin logs in
	 *	b) searches some specific links
	 *  c) gathers two of them
	 *  d) builds body up
	 *  e) deletes those selected links
	 */
	@Test
	public void Everything_must_be_ok_FOR_admin() {
		Long toProductId = findToProductId(TestWorkspaces.Standard_plan_and_no_extra_user.ADMIN(), "Product 1 of Workspace-B");

		//user logs in
		Cookies cookies = TestUtils.login(TestWorkspaces.Premium_plan_and_two_extra_users.ADMIN());

		//searches some specific links
		JSONArray linkList = TestFinder.searchLinks(cookies, "PROBLEM");

		assertNotNull(linkList);
		assertEquals(2, linkList.length());

		//gathers two of them
		Long[] linkIds = new Long[2];
		
		for (int i = 0; i < 2; i++) {
			JSONObject link = linkList.getJSONObject(i);
			linkIds[i] = link.getLong("id");
		}

		//builds the body up
		JSONObject body = new JSONObject();
		body.put("toProductId", toProductId);
		body.put("linkIdSet", linkIds);

		//moves those selected links
		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(body)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertEquals("OK", json.getString("reason"));
	}

	private Long findToProductId(JSONObject user, String productName) {
		Cookies cookies = TestUtils.login(user);

		JSONArray products = TestFinder.searchProducts(cookies, productName, 0);
		TestUtils.logout(cookies);

		assertNotNull(products);

		return products.getJSONObject(0).getLong("id");
	}

	private JSONObject callTheService(Long toProductId, Long[] linkIds) {
		return callTheService(TestWorkspaces.Second_standard_plan_and_no_extra_user.ADMIN(), toProductId, linkIds);
	}

	private JSONObject callTheService(JSONObject user, Long toProductId, Long[] linkIds) {
		return callTheService(user, toProductId, linkIds, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long toProductId, Long[] linkIds, int session) {
		Cookies cookies = TestUtils.login(user);
		
		JSONObject body = null;
		if (toProductId != null || linkIds != null) {
			body = new JSONObject();

			if (toProductId != null) body.put("toProductId", toProductId);
			if (linkIds != null && linkIds.length > 0) body.put("linkIdSet", linkIds);
		}

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
