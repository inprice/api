package io.inprice.api.utils;

import java.util.Map;

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
	 * Searches and returns registered accounts by name
	 * 
	 * @param cookies - must be a super user!
	 * @param name - to be searched (as LIKE)
	 * @return
	 */
	public static JSONArray searchAccounts(Cookies cookies, String name) {
		HttpResponse<JsonNode> res = Unirest.post("/sys/accounts/search")
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
		HttpResponse<JsonNode> res = Unirest.post("/links/search")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(new JSONObject()
					.put("statuses", new String[] { status })
				)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");
		JSONArray rows = data.getJSONArray("rows");
		
		return rows;
	}

	public static JSONArray searchGroups(Cookies cookies, String term) {
		HttpResponse<JsonNode> res = Unirest.post("/groups/search")
			.headers(Fixtures.SESSION_0_HEADERS)
			.cookie(cookies)
			.body(new JSONObject()
					.put("term", term)
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
		JSONObject data = json.getJSONObject("data");
		JSONArray rows = data.getJSONArray("rows");
		
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
		JSONObject data = json.getJSONObject("data");
		JSONArray rows = data.getJSONArray("rows");
		
		return rows;
	}

	public static JSONArray searchComments(Cookies cookies, String ticketPriority, int session) {
		Map<String, String> headers = (session == 0 ? Fixtures.SESSION_0_HEADERS : Fixtures.SESSION_1_HEADERS);

		HttpResponse<JsonNode> res = Unirest.post("/tickets/search")
			.headers(headers)
			.cookie(cookies)
			.body(new JSONObject()
					.put("priorities", new String[] { ticketPriority })
				)
			.asJson();

		JSONObject json = res.getBody().getObject();
		JSONObject data = json.getJSONObject("data");
		JSONArray rows = data.getJSONArray("rows");

		JSONObject ticket = rows.getJSONObject(0);
		
		res = Unirest.get("/ticket/{id}")
			.headers(headers)
			.cookie(cookies)
			.routeParam("id", ""+ticket.getLong("id"))
			.asJson();

		json = res.getBody().getObject();
		data = json.getJSONObject("data");

		return data.getJSONArray("commentList");
	}

}
