package io.inprice.api.utils;

import java.util.Map;
import java.util.Set;

import kong.unirest.json.JSONObject;

/**
 * The workspaces and users placed below must be exactly compliance with the records defined in sql files under resources:db/fixtures folder
 * 
 * @author mdpinar
 * @since 2021-04-07
 */
public enum TestWorkspaces {

	Without_a_plan_and_extra_user(
		"Without A Plan and Extra User",
  	"Has one voucher and 1 User, 1 Workspace and 1 System announces. 2 Tickets and 3 Comments (1 is closed)",
  	Set.of("RB5QV6CF"),
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-a.com")
		)
	),

	Second_without_a_plan_and_extra_user(
		"Second - Without A Plan and Extra User",
  	"Has two vouchers and 1 User and 1 System announces",
  	Set.of("MU3XF9NP", "KJ9QF6G7"),
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-as.com")
		)
	),

	Basic_plan_but_no_extra_user(
		"With Basic Plan (Free Use) but No Extra User",
  	"Has 9 active, 7 trying, 5 waiting and 3 problem links. 2 Product and 3 Link alarms, no voucher",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-b.com")
		)
	),

	Starter_plan_and_one_extra_user(
		"With Starter Plan and One Extra User",
		"Has 6 active, 2 trying, 1 waiting and 3 problem links. 1 Product and 1 Link alarms. No voucher",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-c.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@workspace-c.com")
		)
	),

	Standard_plan_and_no_extra_users(
		"With Standard Plan (Vouchered) but No Extra User",
  	"Has no link or voucher. 1 Product 1 Link alarms",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-d.com")
		)
	),

	Standard_plan_and_one_extra_user(
		"With Standard Plan with One Extra User. 1 Ticket",
  	"Has no link, alarm or voucher",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-e.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@workspace-e.com")
		)
	),

	Standard_plan_and_two_extra_users(
		"With Standard Plan and Two Extra Users",
  	"Has 5 active, 1 trying, 1 waiting and 3 problem links. 2 Tickets opened by Viewer and 1 is opened by Admin (in CLOSED status). Three workspace transactions. No alarm or voucher",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-f.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@workspace-f.com"),
			TestRoles.VIEWER, new JSONObject().put("email", "editor@workspace-e.com") // attention pls!
		)
	),

	Pro_plan_with_no_user(
		"With Pro Plan and No User. Two workspace transactions",
  	"Has 2 active and 2 problem links. No alarm or voucher",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-g.com")
		)
	),

	Pro_plan_with_two_extra_users(
		"With Pro Plan and Two Extra Users.",
  	"Has no link, alarm or voucher",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-m.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@workspace-m.com"),
			TestRoles.VIEWER, new JSONObject().put("email", "viewer@workspace-m.com")
		)
	),

	Premium_plan_and_three_pending_users(
		"With Premium Plan and Two Pending Users",
  	"Has no link, alarm or voucher. Apart from admin, other 3 users are in PENDING state",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-h.com"),
			TestRoles.EDITOR, new JSONObject().put("email", "editor@workspace-h.com"), // not exists as a user (new invitation)!
			TestRoles.VIEWER, new JSONObject().put("email", "editor@workspace-c.com") // attention pls!
			//TestRoles.VIEWER, new JSONObject().put("email", "editor@workspace-e.com") // attention pls, this user is actually added in to this workspace!
		)
	),

	Cancelled_Basic_plan_no_link_no_alarm(
		"Cancelled -Basic Plan- No link, No alarm",
  	"Cancelled and has no link, alarm or voucher",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-i.com")
		)
	),

	Cancelled_Starter_plan_30_links_6_alarms(
		"Cancelled -Starter Plan- 30 links, 6 alarms",
  	"Cancelled and 30 links, 6 alarms, No voucher",
  	null,
		Map.of(
			TestRoles.ADMIN, new JSONObject().put("email", "admin@workspace-j.com")
		)
	);

	private String name; //is used for searching by name in TestFinder.searchWorkspaces(String name)
	private String description;
	private Set<String> vouchers;
	private Map<TestRoles, JSONObject> roleEmailMap;
	
	private TestWorkspaces(String name, String description, Set<String> vouchers, Map<TestRoles, JSONObject> roleEmailMap) {
		this.name = name;
		this.description = description;
		this.vouchers = vouchers;
		this.roleEmailMap = roleEmailMap;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Set<String> getVouchers() {
		return vouchers;
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
