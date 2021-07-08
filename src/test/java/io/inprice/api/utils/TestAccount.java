package io.inprice.api.utils;

/**
 * The accounts and users placed below must be exactly the same as defined in sql files under resources:db/instant folder
 * 
 * @author mdpinar
 * @since 2021-04-07
 */
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import kong.unirest.json.JSONObject;

public enum TestAccount {

	Without_a_plan_and_extra_user(
  	"Has one coupon",
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-a.com")
		)
	),

	Second_without_a_plan_and_extra_user(
  	"Has one coupon",
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-as.com")
		)
	),

	Basic_plan_but_no_extra_user(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-b.com")
		)
	),

	Starter_plan_and_one_extra_user(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-c.com"),
			TestRole.EDITOR, new JSONObject().put("email", "editor@account-c.com")
		)
	),

	Standard_plan_and_no_extra_users(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-d.com")
		)
	),

	Standard_plan_and_one_extra_user(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-e.com"),
			TestRole.EDITOR, new JSONObject().put("email", "editor@account-e.com")
		)
	),

	Standard_plan_and_two_extra_users(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-f.com"),
			TestRole.EDITOR, new JSONObject().put("email", "editor@account-f.com"),
			TestRole.VIEWER, new JSONObject().put("email", "editor@account-e.com") // attention pls!
		)
	),

	Pro_plan_but_no_extra_user(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-g.com")
		)
	);

	private String description;
	private Map<TestRole, JSONObject> roleEmailMap;
	
	private TestAccount(String description, Map<TestRole, JSONObject> roleEmailMap) {
		this.roleEmailMap = roleEmailMap;
	}
	
	public String getDescription() {
		return description;
	}

	public JSONObject ADMIN() {
		return findUser(TestRole.ADMIN);
	}

	public JSONObject EDITOR() {
		return findUser(TestRole.EDITOR);
	}

	public JSONObject VIEWER() {
		return findUser(TestRole.VIEWER);
	}

	public JSONObject findUser(TestRole role) {
		JSONObject user = this.roleEmailMap.get(role);
		if (user != null) {
			user.put("password", "1234");
		}
		return user;
	}

	public String getEmail(TestRole role) {
		JSONObject user = this.roleEmailMap.get(role);
		if (user != null) {
			return user.getString("email");
		}
		return null;
	}

}
