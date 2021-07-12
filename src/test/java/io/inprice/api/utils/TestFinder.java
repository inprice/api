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

}
