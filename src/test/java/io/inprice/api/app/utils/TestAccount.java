package io.inprice.api.app.utils;

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

	Without_a_plan_and_extra_user(1l,
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-a.com")
		)
	),

	Basic_plan_but_no_extra_user(2l,
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-b.com")
		)
	),

	Starter_plan_and_one_extra_user(3l,
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-c.com"),
			TestRole.EDITOR, new JSONObject().put("email", "editor@account-c.com")
		)
	),

	Standard_plan_and_no_extra_users(4l,
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-d.com")
		)
	),

	Standard_plan_and_one_extra_user(5l,
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-e.com"),
			TestRole.EDITOR, new JSONObject().put("email", "editor@account-e.com")
		)
	),

	Standard_plan_and_two_extra_users(6l,
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-f.com"),
			TestRole.EDITOR, new JSONObject().put("email", "editor@account-f.com"),
			TestRole.VIEWER, new JSONObject().put("email", "viewer@account-f.com")
		)
	),

	Pro_plan_but_no_extra_user(7l,
		ImmutableMap.of(
			TestRole.ADMIN, new JSONObject().put("email", "admin@account-g.com")
		)
	);

	private Long id;
	private Map<TestRole, JSONObject> roleEmailMap;
	
	private TestAccount(Long id, Map<TestRole, JSONObject> roleEmailMap) {
		this.id = id;
		this.roleEmailMap = roleEmailMap;
	}
	
	public Long getId() {
		return id;
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
