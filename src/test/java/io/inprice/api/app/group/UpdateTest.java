package io.inprice.api.app.group;

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
 * Tests the functionality of GroupController.update(GroupDTO)
 * 
 * @author mdpinar
 * @since 2021-07-20
 */
@RunWith(JUnit4.class)
public class UpdateTest {

	private static final String SERVICE_ENDPOINT = "/group";

	private static final JSONObject SAMPLE_BODY = 
			new JSONObject()
				.put("id", 1)
  			.put("name", "NEW GROUP")
	    	.put("description", "THIS IS ANOTHER GROUP")
				.put("price", 5);

	@BeforeClass
	public static void setup() {
		TestUtils.setup();
	}

	@Test
	public void No_active_session_please_sign_in_WITHOUT_login() {
		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
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
	public void Group_not_found_WITHOUT_id() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("id");
		
		JSONObject json = callTheService(body);

		assertEquals(404, json.getInt("status"));
		assertEquals("Group not found!", json.getString("reason"));
	}

	@Test
	public void Name_cannot_be_empty() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.remove("name");
		
		JSONObject json = callTheService(body);

		assertEquals(400, json.getInt("status"));
		assertEquals("Name cannot be empty!", json.getString("reason"));
	}

	@Test
	public void Name_must_be_between_3_and_50_chars_WITH_shorter_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("name", "AB");

		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
    assertEquals("Name must be between 3 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Name_must_be_between_3_and_50_chars_WITH_longer_name() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("name", RandomStringUtils.randomAlphabetic(51));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Name must be between 3 - 50 chars!", json.getString("reason"));
	}

	@Test
	public void Description_can_be_up_to_128_chars_WITH_longer_description() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("description", RandomStringUtils.randomAlphabetic(129));
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Description can be up to 128 chars!", json.getString("reason"));
	}

	@Test
	public void Price_is_out_of_reasonable_range_FOR_a_value_of_less_than_zero() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("price", -5.0);
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Price is out of reasonable range!", json.getString("reason"));
	}

	@Test
	public void Price_is_out_of_reasonable_range_FOR_a_value_of_10m() {
		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("price", 10_000_000);
		
		JSONObject json = callTheService(body);
		
		assertEquals(400, json.getInt("status"));
		assertEquals("Price is out of reasonable range!", json.getString("reason"));
	}

	@Test
	public void You_are_not_allowed_to_do_this_operation_WITH_superuser() {
		JSONObject json = callTheService(Fixtures.SUPER_USER, SAMPLE_BODY, 0);

		assertEquals(511, json.getInt("status"));
		assertEquals("You are not allowed to do this operation!", json.getString("reason"));
	}

	@Test
	public void Forbidden_WITH_viewer() {
		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.VIEWER(), SAMPLE_BODY, 1);

		assertEquals(403, json.getInt("status"));
		assertEquals("Forbidden!", json.getString("reason"));
	}

	@Test
	public void You_already_have_a_group_having_the_same_name() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.ADMIN());
		
		JSONArray groupList = TestFinder.searchGroups(cookies, "Group G");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(groupList);
		assertEquals(1, groupList.length());
		
		JSONObject group = groupList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", group.getLong("id")); //here is also important!
		body.put("name", "Group K of Account-F");

		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.ADMIN(), body, 0);

		assertEquals(875, json.getInt("status"));
		assertEquals("You already have a group having the same name!", json.getString("reason"));
	}

	@Test
	public void Everything_must_be_ok_WITH_editor() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_two_extra_users.EDITOR());
		
		JSONArray groupList = TestFinder.searchGroups(cookies, "Group I");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(groupList);
		assertEquals(1, groupList.length());
		
		JSONObject group = groupList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", group.getLong("id")); //here is also important!
		body.put("name", "Changed name by EDITOR");
		body.put("description", "This is a changed description!");
		body.put("price", 20.12);

		JSONObject json = callTheService(TestAccounts.Standard_plan_and_two_extra_users.EDITOR(), body, 0);

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONObject data = json.getJSONObject("data");
		assertTrue(data.has("group"));
	}

	@Test
	public void Everything_must_be_ok_WITH_admin() {
		Cookies cookies = TestUtils.login(TestAccounts.Standard_plan_and_one_extra_user.ADMIN());
		
		JSONArray groupList = TestFinder.searchGroups(cookies, "Group R");
		TestUtils.logout(cookies); //here is important!

		assertNotNull(groupList);
		assertEquals(1, groupList.length());
		
		JSONObject group = groupList.getJSONObject(0);

		JSONObject body = new JSONObject(SAMPLE_BODY.toMap());
		body.put("id", group.getLong("id")); //here is also important!
		body.put("name", "Changed name by ADMIN");
		body.put("description", "This description isn't descriptive enough!");
		body.put("price", 0.12);

		JSONObject json = callTheService(TestAccounts.Standard_plan_and_one_extra_user.ADMIN(), body, 0);

		assertEquals(200, json.getInt("status"));
		assertTrue(json.has("data"));

		JSONObject data = json.getJSONObject("data");
		assertTrue(data.has("group"));
	}

	private JSONObject callTheService(JSONObject body) {
		return callTheService(TestAccounts.Pro_plan_with_no_user.ADMIN(), body, 0);
	}
	
	private JSONObject callTheService(JSONObject user, JSONObject body, int session) {
		Cookies cookies = TestUtils.login(user);

		HttpResponse<JsonNode> res = Unirest.put(SERVICE_ENDPOINT)
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS) //attention pls!
			.cookie(cookies)
			.body(body != null ? body : "")
			.asJson();
		TestUtils.logout(cookies);

		return res.getBody().getObject();
	}

}
