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
 * Tests the functionality of ProductController.getIdNameList(Long excludedProductId)
 * 
 * @author mdpinar
 * @since 2021-07-20
 */
@RunWith(JUnit4.class)
public class GetIdNameListTest {

	private static final String SERVICE_ENDPOINT = "/product/pairs/{excludedProductId}";

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

	@Test
	public void You_must_bind_an_workspace_WITH_superuser_WITHOUT_binding_workspace() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, 1L);

		assertEquals(915, json.getInt("status"));
		assertEquals("You must bind to a workspace!", json.getString("reason"));
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
		
		JSONArray workspaceList = TestFinder.searchWorkspaces(cookies, TestWorkspaces.Second_standard_plan_and_no_extra_user.getName());
		JSONObject workspace = workspaceList.getJSONObject(0);

		HttpResponse<JsonNode> res = Unirest.put("/sys/workspace/bind/{workspaceId}")
			.cookie(cookies)
			.routeParam("workspaceId", ""+workspace.getLong("id"))
			.asJson();

		JSONObject json = res.getBody().getObject();
		assertEquals(200, json.getInt("status"));

		JSONArray productList = TestFinder.searchProducts(cookies, "Product D", 0);
		JSONObject product = productList.getJSONObject(0);
		
		res = Unirest.get(SERVICE_ENDPOINT)
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("excludedProductId", ""+product.getLong("id"))
			.asJson();
		TestUtils.logout(cookies);

		json = res.getBody().getObject();

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));
	}

	@Test
	public void Everything_must_be_ok_FOR_any_kind_of_users() {
		Map<TestRoles, JSONObject> roleUserMap = Map.of(
			TestRoles.ADMIN, TestWorkspaces.Premium_plan_and_two_extra_users.ADMIN(),
			TestRoles.EDITOR, TestWorkspaces.Premium_plan_and_two_extra_users.EDITOR(),
			TestRoles.VIEWER, TestWorkspaces.Premium_plan_and_two_extra_users.VIEWER()
		);

		for (Entry<TestRoles, JSONObject> roleUser: roleUserMap.entrySet()) {
			JSONObject json = callTheService(roleUser.getValue(), 0L, 0);

			assertEquals(200, json.getInt("status"));
			assertTrue(json.has("data"));

			JSONArray data = json.getJSONArray("data");
			assertEquals(roleUser.getKey().name(), 3, data.length());
		}
	}

	@Test
	public void Everything_must_be_ok_WITH_excluded_id() {
		Cookies cookies = TestUtils.login(TestWorkspaces.Professional_plan_and_one_extra_user.EDITOR());

		JSONArray productList = TestFinder.searchProducts(cookies, "Product X", 0);
		TestUtils.logout(cookies); //here is important!
		
		assertNotNull(productList);
		JSONObject product = productList.getJSONObject(0);

		//evil user tries to find the product
		JSONObject json = callTheService(TestWorkspaces.Professional_plan_and_one_extra_user.EDITOR(), product.getLong("id"));

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONArray data = json.getJSONArray("data");
		assertEquals(1, data.length());
		assertTrue(data.getJSONObject(0).getString("right").startsWith("Product Y"));
	}

	private JSONObject callTheService(Long excludedProductId) {
		return callTheService(TestWorkspaces.Standard_plan_and_no_extra_user.ADMIN(), excludedProductId);
	}

	private JSONObject callTheService(JSONObject user, Long excludedProductId) {
		return callTheService(user, excludedProductId, 0);
	}
	
	private JSONObject callTheService(JSONObject user, Long excludedProductId, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.get(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //for allowing viewers
			.cookie(cookies)
			.routeParam("excludedProductId", (excludedProductId != null ? excludedProductId.toString() : ""))
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
