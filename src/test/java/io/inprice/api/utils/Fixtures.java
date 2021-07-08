package io.inprice.api.utils;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import kong.unirest.json.JSONObject;

public class Fixtures {

	public static final JSONObject SUPER_USER = new JSONObject().put("email", "super@inprice.io").put("password", "1234");
	public static final JSONObject BANNED_USER = new JSONObject().put("email", "banned@inprice.io").put("password", "1234");
	
	public static final JSONObject USER_HAVING_TWO_MEMBERSHIPS = new JSONObject().put("email", "editor@account-e.com").put("password", "1234");
	
	public static final String NON_EXISTING_EMAIL_1 = "non-existing-1@user.com";
	public static final String NON_EXISTING_EMAIL_2 = "non-existing-2@user.com";
	
	public static final Map<String, String> SESSION_O_HEADERS = ImmutableMap.of("X-Session", "0");
	public static final Map<String, String> SESSION_1_HEADERS = ImmutableMap.of("X-Session", "1");

	public static final String COUPON_P10_1 = "EA8XG2S7";
	public static final String COUPON_P15_1 = "US8PN1X9";
	public static final String COUPON_P15_2 = "FH5GV9AA";
	public static final String COUPON_P20_1 = "YF4GZ5SG";
	public static final String COUPON_P20_2 = "NE9FC64B";	
}
