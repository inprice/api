package io.inprice.api.app.product;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.inprice.api.utils.Fixtures;
import io.inprice.api.utils.TestWorkspaces;
import io.inprice.api.utils.TestFinder;
import io.inprice.api.utils.TestRoles;
import io.inprice.api.utils.TestUtils;
import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Tests the functionality of ProductController.findLinksById(Long productId)
 * 
 * @author mdpinar
 * @since 2021-07-20
 */
@RunWith(JUnit4.class)
public class FindLinksByIdTest {

	private static final String SERVICE_ENDPOINT = "/product/links/{id}";

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void Request_body_is_invalid_WITH_null_id() {
		JSONObject json = callTheService(null);

		assertEquals(400, json.getInt("status"));
    assertEquals("Request body is invalid!", json.getString("reason"));
	}

	/**
	 * Consists of four steps;
	 *	a) to gather other workspace's products, admin is logged in
	 *	b) finds some specific products
	 *  c) picks one of them
	 *  d) evil user tries to find the product
	 */
	@Test
	public void Product_not_found_WHEN_trying_to_find_someone_elses_product() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Starter_plan_and_one_extra_user.ADMIN());

		JSONArray productList = TestFinder.searchProducts(cookies, "Product X");
		TestUtils.logout(cookies); //here is important!
		
		assertNotNull(productList);
		JSONObject product = productList.getJSONObject(0);

		//evil user tries to find the product
		JSONObject json = callTheService(product.getLong("id"));

		assertEquals(404, json.getInt("status"));
		assertEquals("Product not found!", json.getString("reason"));
	}

	@Test
	public void You_must_bind_an_workspace_WITH_superuser_WITHOUT_binding_workspace() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind an workspace!", json.getString("reason"));
	}

	/**
	 * Consists of three steps;
	 * 	a) super user logs in
	 * 	b) binds to a specific workspace
	 * 	c) gets product list (must not be empty)
	 */
	@Test
	public void Everything_must_be_ok_WITH_superuser_AND_bound_workspace() {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);
		
		JSONArray workspaceList = TestFinder.searchWorkspaces(cookies, "With Standard Plan (Vouchered) but No Extra User");
		JSONObject workspace = workspaceList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put("/sys/workspace/bind/{workspaceId}")
			.cookie(cookies)
			.routeParam("workspaceId", ""+workspace.getLong("id"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		JSONArray productList = TestFinder.searchProducts(cookies, "Product D");
		JSONObject product = productList.getJSONObject(0);
		
		res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+product.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		JSONObject data = json.getJSONObject("data");
		
		assertTrue(data.has("product"));
		assertTrue(data.has("links"));
		
		JSONArray links = data.getJSONArray("links");
		assertEquals(1, links.length());
	}

	@Test
	public void Everything_must_be_ok_FOR_any_kind_of_users() {
		Map<TestRoles, JSONObject> roleUserMap = Map.of(
			TestRoles.ADMIN, TestWorkspaces.Standard_plan_and_two_extra_users.ADMIN(),
			TestRoles.EDITOR, TestWorkspaces.Standard_plan_and_two_extra_users.EDITOR(),
			TestRoles.VIEWER, TestWorkspaces.Standard_plan_and_two_extra_users.VIEWER()
		);

		Cookies cookies = TestUtils.login(roleUserMap.get(TestRoles.ADMIN));

		JSONArray productList = TestFinder.searchProducts(cookies, "Product G");
		TestUtils.logout(cookies);

		assertNotNull(productList);
		assertEquals(1, productList.length());

		//get the first product
		JSONObject product = productList.getJSONObject(0);

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue(), product.getLong("id"), (TestRoles.VIEWER.equals(roleUser.getKey()) ? 1 : 0));

			assertEquals(roleUser.getKey().name(), 200, json.getInt("status"));
			assertTrue(json.has("data"));

			JSONObject data = json.getJSONObject("data");
			
			assertTrue(data.has("product"));
			assertTrue(data.has("links"));
  		
  		JSONArray links = data.getJSONArray("links");
  		assertEquals(4, links.length());
		}
	}

	private JSONObject callTheService(Long id) {
		return callTheService(TestWorkspaces.Basic_plan_but_no_extra_user.ADMIN(), id);
	}

	private JSONObject callTheService(JSONObject user, Long id) {
		return callTheService(user, id, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long id, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.routeParam("id", (id != null ? id.toString() : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
