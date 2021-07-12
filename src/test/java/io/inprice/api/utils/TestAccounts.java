package io.inprice.api.utils;

import java.util.Map;
import kong.unirest.json.JSONObject;
import com.google.common.collect.ImmutableMap;

/**
 * The accounts and users placed below must be exactly the same as defined in sql files under resources:db/instant folder
 * 
 * @author mdpinar
 * @since 2021-04-07
 */
public enum TestAccounts {

	Without_a_plan_and_extra_user(
  	"Has one coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-a.com")
		)
	),

	Second_without_a_plan_and_extra_user(
  	"Has one coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-as.com")
		)
	),

	Basic_plan_but_no_extra_user(
  	"Has 9 active, 7 trying, 5 waiting and 3 problem links. No alarm or coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-b.com")
		)
	),

	Starter_plan_and_one_extra_user(
		"Has 6 active, 2 trying, 1 waiting and 3 problem links. No alarm or coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-c.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@account-c.com")
		)
	),

	Standard_plan_and_no_extra_users(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-d.com")
		)
	),

	Standard_plan_and_one_extra_user(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-e.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@account-e.com")
		)
	),

	Standard_plan_and_two_extra_users(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-f.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@account-f.com"),
			TestRoles.VIEWER, new JSONObject().put("email", "editor@account-e.com") // attention pls!
		)
	),

	Pro_plan_with_no_user(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-g.com")
		)
	),

	Premium_plan_and_three_pending_users(
  	"Has no link, alarm or coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-h.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@account-h.com"), // not exists as a user (new invitation)!
			TestRoles.VIEWER, new JSONObject().put("email", "editor@account-c.com") // attention pls!
			//TestRoles.VIEWER, new JSONObject().put("email", "editor@account-e.com") // attention pls!
		)
	),

	Cancelled_Basic_plan_no_link_no_alarm(
  	"Cancelled and has no link, alarm or coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-i.com")
		)
	),

	Cancelled_Starter_plan_30_links_6_alarms(
  	"Cancelled and 30 links, 6 alarms, No coupon",
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-j.com")
		)
	);

	private String description;
	private Map<TestRoles, JSONObject> roleEmailMap;
	
	private TestAccounts(String description, Map<TestRoles, JSONObject> roleEmailMap) {
		this.roleEmailMap = roleEmailMap;
	}
	
	public String getDescription() {
		return description;
	}

	public JSONObject ADMIN() {
		return findUser(TestRoles.ADMIN);
	}

	public JSONObject EDITOR() {
		return findUser(TestRoles.EDITOR);
	}

	public JSONObject VIEWER() {
		return findUser(TestRoles.VIEWER);
	}

	public JSONObject findUser(TestRoles role) {
		JSONObject user = this.roleEmailMap.get(role);
		if (user != null) {
			user.put("password", "1234");
		}
		return user;
	}

	public String getEmail(TestRoles role) {
		JSONObject user = this.roleEmailMap.get(role);
		if (user != null) {
			return user.getString("email");
		}
		return null;
	}

}
