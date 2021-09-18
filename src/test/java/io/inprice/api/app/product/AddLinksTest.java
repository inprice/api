package io.inprice.api.app.product;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
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
 * Tests the functionality of ProductController.addLinks(AddLinksDTO)
 * 
 * @author mdpinar
 * @since 2021-07-22
 */
@RunWith(JUnit4.class)
public class AddLinksTest {

	private static final String SERVICE_ENDPOINT = "/product/add-links";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
				.put("productId", 1)
  			.put("linksText", "https://url-1.com/abc");

	private static final String VALID_URLS = 
				"https://test-url-1.com/ax-1\n" +
				"https://test-url-2.com/mq-0\n" +
				"https://test-url-3.com/aa-7\n" +
				"\n" +
				"https://test-url-4.com/sp-a";

	private static final String INVALID_URLS = 
				"ftp://file-url-1.com/941\n" +
				"udp://channel-2.broadcast.fm\n" +
				"rdp://62.12.44.10:8078/acm3-44\n" +
				"http://wrong";

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
	public void Product_not_found_WITHOUT_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("productId");
		
		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Product not found!", json.getString("reason"));
	}

	@Test
	public void URL_list_is_empty_WITOUT_linksText() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("linksText");
		
		JSONObject json = callTheService(body);

		assertEquals(1003, json.getInt("status"));
		assertEquals("URL list is empty!", json.getString("reason"));
	}
	
	@Test
	public void URL_list_is_empty_WITH_empty_linksText() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("linksText", "\n\n\n");
		
		JSONObject json = callTheService(body);
		
		assertEquals(1003, json.getInt("status"));
		assertEquals("URL list is empty!", json.getString("reason"));
	}

	@Test
	public void Mostly_invalid_URLs_WITH_half_wrong_list() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("linksText", VALID_URLS + INVALID_URLS);

		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Mostly invalid URLs!", json.getString("reason"));
	}

	@Test
	public void Mostly_invalid_URLs_WITH_full_wrong_list() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("linksText", INVALID_URLS);

		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Mostly invalid URLs!", json.getString("reason"));
	}

	@Test
	public void Invalid_URLs_at() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("linksText", "https://invalid-url-1.\n" + "ttps://invalid-url2.com\n" + VALID_URLS);

		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Invalid URL(s) at 1, 2", json.getString("reason"));
	}
	
	@Test
	public void You_havent_picked_a_plan_yet() {
		JSONObject json = callTheService(TestAccounts.Second_without_a_plan_and_extra_user.ADMIN(), SAMPLE_BODY, 0);

		assertEquals(903, json.getInt("status"));
		assertEquals("You haven't picked a plan yet!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), SAMPLE_BODY, 1);

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_can_add_up_to_X_links() {
		Cookies cookies = TestUtils.login(TestAccounts.Basic_plan_but_no_extra_user.ADMIN());
		
		JSONArray productList = TestFinder.searchProducts(cookies, "Product 2");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(productList);
		assertEquals(1, productList.length());
		
		JSONObject product = productList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("productId", product.getLong("id")); //here is also important!
		body.put("linksText", VALID_URLS);

		JSONObject json = callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), body, 0);

		assertEquals(400, json.getInt("status"));
		assertEquals("You can add up to 1 link(s)!", json.getString("reason"));
	}

	@Test
	public void You_are_allowed_to_upload_up_to_100_URLs_at_once() {
		Cookies cookies = TestUtils.login(TestAccounts.Pro_plan_with_no_user.ADMIN());
		
		JSONArray productList = TestFinder.searchProducts(cookies, "Product A");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(productList);
		assertEquals(1, productList.length());
		
		JSONObject product = productList.getJSONObject(0);
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i <= 100; i++) {
			sb.append(
				String.format("https://amazon.com/%s\n", RandomStringUtils.randomAlphabetic(4))
			);
		}

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("productId", product.getLong("id")); //here is also important!
		body.put("linksText", sb.toString());

		JSONObject json = callTheService(TestAccounts.Basic_plan_but_no_extra_user.ADMIN(), body, 0);

		assertEquals(902, json.getInt("status"));
		assertEquals("You are allowed to upload up to 100 URLs at once!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_one_extra_user.EDITOR());
		
		JSONArray productList = TestFinder.searchProducts(cookies, "Product R");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(productList);
		assertEquals(1, productList.length());
		
		JSONObject product = productList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("productId", product.getLong("id")); //here is also important!
		body.put("linksText", "https://blue-dot.com/xsa-123");

		JSONObject json = callTheService(TestAccounts.Standard_plan_and_one_extra_user.EDITOR(), body, 0);

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONObject data = json.getJSONObject("data");
		assertTrue(data.has("product"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_one_extra_user.ADMIN());
		
		JSONArray productList = TestFinder.searchProducts(cookies, "Product S");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(productList);
		assertEquals(1, productList.length());
		
		JSONObject product = productList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("productId", product.getLong("id")); //here is also important!
		body.put("linksText", "https://red-planet.com/mars/aa123");

		JSONObject json = callTheService(TestAccounts.Standard_plan_and_one_extra_user.ADMIN(), body, 0);

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONObject data = json.getJSONObject("data");
		assertTrue(data.has("product"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestAccounts.Pro_plan_with_no_user.ADMIN(), body, 0);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.post(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //attention pls!
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
