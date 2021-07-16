package io.inprice.api.utils;

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

}
