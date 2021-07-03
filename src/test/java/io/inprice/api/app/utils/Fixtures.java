package io.inprice.api.app.utils;

import kong.unirest.json.JSONObject;

public class Fixtures {

	public static final JSONObject SUPER_USER = new JSONObject().put("email", "super@inprice.io").put("password", "1234");
	public static final JSONObject BANNED_USER = new JSONObject().put("email", "banned@inprice.io").put("password", "1234");

	public static JSONObject NORMAL_USER(TestRole role) {
		return NORMAL_USER(role, TestAccount.X);
	}

	public static JSONObject NORMAL_USER(TestRole role, TestAccount account) {
		String email = String.format("%s@acme-%s.com", role.name().toLowerCase(), account.name().toLowerCase());
		return new JSONObject().put("email", email).put("password", "1234");
	}

}
