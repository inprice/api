package io.inprice.api.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import kong.unirest.Cookies;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

/**
 * Finds records on test db to ease test steps
 * 
 * @author mdpinar
 * @since 2021-07-12
 */
public class TestFinder {

	/**
	 * Searches and returns registered workspaces by name
	 * 
	 * @param cookies - must be a super user!
	 * @param name - to be searched (as LIKE)
	 * @return
	 */
	public static JSONArray searchWorkspaces(Cookies cookies, String name) {
		HttpResponse<JsonNode> res = Unirest.post("/sys/workspaces/search")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(new JSONObject()
					.put("term", name)
				)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");
		
		return data;
	}

	public static JSONArray searchLinks(Cookies cookies, String status) {
		return searchLinks(cookies, status, 0);
	}

	public static JSONArray searchLinks(Cookies cookies, String status, int session) {
		HttpResponse<JsonNode> res = Unirest.post("/links/search")
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.body(new JSONObject()
					.put("statuses", new String[] { status })
				)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray rows = json.getJSONArray("data");

		return rows;
	}

	/**
	 * Searches products by name
	 * 
	 * @param cookies
	 * @param term - LIKE by name
	 * @return
	 */
	public static JSONArray searchProducts(Cookies cookies, String byName, int session) {
		HttpResponse<JsonNode> res = Unirest.post("/products/search")
				.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.body(new JSONObject()
					.put("term", byName)
				)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");
		
		return data;
	}

	public static JSONArray searchAlarms(Cookies cookies, String topic) {
		HttpResponse<JsonNode> res = Unirest.post("/alarms/search")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(new JSONObject().put("topic", topic))
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray rows = json.getJSONArray("data");
		
		return rows;
	}

	public static JSONArray searchTickets(Cookies cookies, String[] priorities, int session) {
		HttpResponse<JsonNode> res = Unirest.post("/tickets/search")
			.headers(session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS)
			.cookie(cookies)
			.body(new JSONObject()
					.put("priorities", priorities)
				)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray rows = json.getJSONArray("data");
		
		return rows;
	}

	public static JSONArray searchComments(Cookies cookies, String ticketPriority) {
		HttpResponse<JsonNode> res = Unirest.post("/tickets/search")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(new JSONObject()
					.put("priorities", new String[] { ticketPriority })
				)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray rows = json.getJSONArray("data");

		JSONObject ticket = rows.getJSONObject(0);
		
		res = Unirest.get("/ticket/{id}")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();

		json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");

		return data.getJSONArray("commentList");
	}

	public static JSONArray getMembers(Cookies cookies) {
		HttpResponse<JsonNode> res = Unirest.get("/membership")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");

		return data;
	}

	public static JSONArray searchUsers(String byEmail) {
		Cookies cookies = TestUtils.login(Fixtures.SUPER_USER);

		HttpResponse<JsonNode> res = Unirest.post("/sys/users/search")
			.cookie(cookies)
			.body(new JSONObject()
					.put("term", byEmail)
				)
			.asJson();
		TestUtils.logout(cookies);

		JSONObject json = res.getBody().getObject();
		JSONArray data = json.getJSONArray("data");

		assertEquals(200, json.getInt("status"));
		assertNotNull(data);
		
		return data;
	}

}
