package io.inprice.api.utils;

import java.util.Map;
import java.util.Set;

import kong.unirest.json.JSONObject;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableMap;

/**
 * The accounts and users placed below must be exactly compliance with the records defined in sql files under resources:db/fixtures folder
 * 
 * @author mdpinar
 * @since 2021-04-07
 */
public enum TestAccounts {

	Without_a_plan_and_extra_user(
		"Without A Plan and Extra User",
  	"Has one coupon and 1 User, 1 Account and 1 System announces. 2 Tickets and 3 Comments (1 is closed)",
  	ImmutableSet.of("RB5QV6CF"),
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-a.com")
		)
	),

	Second_without_a_plan_and_extra_user(
		"Second - Without A Plan and Extra User",
  	"Has two coupons and 1 User and 1 System announces",
  	ImmutableSet.of("MU3XF9NP", "KJ9QF6G7"),
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-as.com")
		)
	),

	Basic_plan_but_no_extra_user(
		"With Basic Plan (Free Use) but No Extra User",
  	"Has 9 active, 7 trying, 5 waiting and 3 problem links. 2 Group and 3 Link alarms, no coupon",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-b.com")
		)
	),

	Starter_plan_and_one_extra_user(
		"With Starter Plan and One Extra User",
		"Has 6 active, 2 trying, 1 waiting and 3 problem links. 1 Group and 1 Link alarms. No coupon",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-c.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@account-c.com")
		)
	),

	Standard_plan_and_no_extra_users(
		"With Standard Plan (Couponed) but No Extra User",
  	"Has no link or coupon. 1 Group 1 Link alarms",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-d.com")
		)
	),

	Standard_plan_and_one_extra_user(
		"With Standard Plan with One Extra User. 1 Ticket",
  	"Has no link, alarm or coupon",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-e.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@account-e.com")
		)
	),

	Standard_plan_and_two_extra_users(
		"With Standard Plan and Two Extra Users",
  	"Has 5 active, 1 trying, 1 waiting and 3 problem links. 2 Tickets opened by Viewer and 1 is opened by Admin (in CLOSED status). Three account transactions. No alarm or coupon",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-f.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@account-f.com"),
			TestRoles.VIEWER, new JSONObject().put("email", "editor@account-e.com") // attention pls!
		)
	),

	Pro_plan_with_no_user(
		"With Pro Plan and No User. Two account transactions",
  	"Has 2 active and 2 problem links. No alarm or coupon",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-g.com")
		)
	),

	Pro_plan_with_two_extra_users(
		"With Pro Plan and Two Extra Users.",
  	"Has no link, alarm or coupon",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-m.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@account-m.com"),
			TestRoles.VIEWER, new JSONObject().put("email", "viewer@account-m.com")
		)
	),

	Premium_plan_and_three_pending_users(
		"With Premium Plan and Two Pending Users",
  	"Has no link, alarm or coupon. Apart from admin, other 3 users are in PENDING state",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-h.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@account-h.com"), // not exists as a user (new invitation)!
			TestRoles.VIEWER, new JSONObject().put("email", "editor@account-c.com") // attention pls!
			//TestRoles.VIEWER, new JSONObject().put("email", "editor@account-e.com") // attention pls, this user is actually added in to this account!
		)
	),

	Cancelled_Basic_plan_no_link_no_alarm(
		"Cancelled -Basic Plan- No link, No alarm",
  	"Cancelled and has no link, alarm or coupon",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-i.com")
		)
	),

	Cancelled_Starter_plan_30_links_6_alarms(
		"Cancelled -Starter Plan- 30 links, 6 alarms",
  	"Cancelled and 30 links, 6 alarms, No coupon",
  	null,
		ImmutableMap.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@account-j.com")
		)
	);

	private String name; //is used for searching by name in TestFinder.searchAccounts(String name)
	private String description;
	private Set<String> coupons;
	private Map<TestRoles, JSONObject> roleEmailMap;
	
	private TestAccounts(String name, String description, Set<String> coupons, Map<TestRoles, JSONObject> roleEmailMap) {
		this.name = name;
		this.description = description;
		this.coupons = coupons;
		this.roleEmailMap = roleEmailMap;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Set<String> getCoupons() {
		return coupons;
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
