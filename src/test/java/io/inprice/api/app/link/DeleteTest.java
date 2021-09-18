package io.inprice.api.app.link;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestAccounts;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of LinkController.delete(LinkDeleteDTO)
 * 
 * @author mdpinar
 * @since 2021-07-18
 */
@RunWith(JUnit4.class)
public class DeleteTest {

	private static final String SERVICE_ENDPOINT = "/link";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
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
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), 1L, new Long[] { 1L }, 1);

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	/**
	 * Consists of six steps;
	 *	a) to gather other account's links, admin is logged in
	 *	b) searches some specific links
	 *  c) picks one of those links
	 *  d) builds body up
	 *  e) evil user logs in
	 *  f) tries to delete other account's links
	 */
	@Test
	public void Link_not_found_WHEN_trying_to_delete_someone_elses_links() {
		//to gather other account's links, admin is logged in
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());

		//searches some specific links
		JSONArray linkList = TestFinder.searchLinks(cookies, "ACTIVE");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(linkList);

		//picks one of those links
		JSONObject link = linkList.getJSONObject(0);
		Long[] linkIds = { link.getLong("id") };
		Long fromProductId = link.getLong("productId");

		//evil user logs in
		cookies = TestUtils.login(TestAccounts.Standard_plan_and_one_extra_user.EDITOR());

		//builds the body up
		JSONObject body = new JSONObject();
		body.put("fromProductId", fromProductId);
		body.put("linkIdSet", linkIds);

		//tries to delete other users' links
		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
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
	public void Everything_must_be_ok_FOR_editor_and_admin() {
		//both account have 2 links in PROBLEM status!
		JSONObject[] users = {
			TestAccounts.Standard_plan_and_two_extra_users.ADMIN(),
			TestAccounts.Standard_plan_and_one_extra_user.EDITOR()
		};

		for (JSONObject user: users) {
  		//user logs in
  		Cookies cookies = TestUtils.login(user);
  
  		//searches some specific links
  		JSONArray linkList = TestFinder.searchLinks(cookies, "PROBLEM");
  
  		assertNotNull(linkList);
  		assertEquals(2, linkList.length());
  
  		//gathers two of them
  		Long fromProductId = null;
  		Long[] linkIds = new Long[2];
  		
  		for (int i = 0; i < 2; i++) {
  			JSONObject link = linkList.getJSONObject(i);
  			linkIds[i] = link.getLong("id");
  			fromProductId = link.getLong("productId");
  		}

  		assertNotNull(fromProductId);

  		//builds the body up
  		JSONObject body = new JSONObject();
  		body.put("fromProductId", fromProductId);
  		body.put("linkIdSet", linkIds);
  
  		//deletes those selected links
  		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
  			.headers(Fixtures.SESSION_0_HEADERS)
  			.cookie(cookies)
  			.body(body)
  			.asJson();
  		TestUtils.logout(cookies);
  
  		JSONObject json = res.getBody().getObject();
  
  		assertEquals(200, json.getInt("status"));
  		assertTrue(json.has("data"));

  		JSONObject data = json.getJSONObject("data");
  		assertTrue(data.has("product"));
		}
	}

	private JSONObject callTheService(Long productId, Long[] linkIds) {
		return callTheService(TestAccounts.Standard_plan_and_no_extra_users.ADMIN(), productId, linkIds);
	}

	private JSONObject callTheService(JSONObject user, Long productId, Long[] linkIds) {
		return callTheService(user, productId, linkIds, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long productId, Long[] linkIds, int session) {
		Cookies cookies = TestUtils.login(user);
		
		JSONObject body = null;
		if (productId != null || linkIds != null) {
			body = new JSONObject();

			if (productId != null) body.put("fromProductId", productId);
			if (linkIds != null && linkIds.length > 0) body.put("linkIdSet", linkIds);
		}

		HttpResponse<JsonNode> res = Unirest.delete(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
