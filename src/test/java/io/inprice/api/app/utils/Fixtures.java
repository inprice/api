package io.inprice.api.app.utils;

import kong.unirest.json.JSONObject;

public class Fixtures {

	public static final JSONObject SUPER_USER = new JSONObject().put("email", "super@inprice.io").put("password", "1234");
	public static final JSONObject BANNED_USER = new JSONObject().put("email", "banned@inprice.io").put("password", "1234");
	
	public static final JSONObject USER_HAVING_TWO_MEMBERSHIPS = new JSONObject().put("email", "editor@account-e.com").put("password", "1234");

}
