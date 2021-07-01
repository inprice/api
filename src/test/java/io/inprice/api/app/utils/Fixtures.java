package io.inprice.api.app.utils;

import kong.unirest.json.JSONObject;

public class Fixtures {
	
	public static final JSONObject SUPER_USER = new JSONObject().put("email", "super@inprice.io").put("password", "1234");
	public static final JSONObject ADMIN_USER = new JSONObject().put("email", "admin@inprice.io").put("password", "1234");
	public static final JSONObject EDITOR_USER = new JSONObject().put("email", "editor@inprice.io").put("password", "1234");
	public static final JSONObject VIEWER_USER = new JSONObject().put("email", "viewer@inprice.io").put("password", "1234");
	public static final JSONObject BANNED_USER = new JSONObject().put("email", "banned@inprice.io").put("password", "1234");

}
