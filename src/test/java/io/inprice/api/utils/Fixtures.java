package io.inprice.api.utils;

import java.util.List;
import java.util.Map;

import kong.unirest.json.JSONObject;

public class Fixtures {

	public static final JSONObject SUPER_USER = new JSONObject().put("email", "super@inprice.io").put("password", "1234-AB");
	public static final JSONObject BANNED_USER = new JSONObject().put("email", "banned@inprice.io").put("password", "1234-AB");
	
	//two are JOINED, one is PENDING!
	public static final JSONObject USER_HAVING_THREE_MEMBERSHIPS = new JSONObject().put("email", "editor@workspace-e.com").put("password", "1234-AB");
	
	public static final String NON_EXISTING_EMAIL_1 = "non-existing-1@user.com";
	public static final String NON_EXISTING_EMAIL_2 = "non-existing-2@user.com";
	
	public static final Map<String, String> SESSION_0_HEADERS = Map.of("X-Session", "0");
	public static final Map<String, String> SESSION_1_HEADERS = Map.of("X-Session", "1");

	public static final List<JSONObject> NORMAL_USER_LIST = List.of(
		TestWorkspaces.Pro_plan_with_two_extra_users.VIEWER(),
		TestWorkspaces.Pro_plan_with_two_extra_users.EDITOR(),
		TestWorkspaces.Pro_plan_with_two_extra_users.ADMIN()
	);

}
